# Demo Site Automation

We publish a (demo version)[https://ppmclala.github.io/crosswords.svg] to Github Pages on every commit to main via GitHub Actions.  The `publish.yml` (workflow)[.github/workflows/publish.yml] implements the following steps:

* `sync`: connect to Google Sheets API via (Direct Workload Identity Federation)[https://github.com/google-github-actions/auth?tab=readme-ov-file#direct-wif] and download puzzle data.
* `build`: validate puzzle data, generate JS code and create final HTML package
* `deploy`: push package to Github Pages

## Actions

The actions use `babashka` and tasks listed in bb.edn and contain two job:

* `build`: runs `bb sync` and `bb build`
* `deploy`: pushes package to Github Pages

### Notes

#### create pool

```
gcloud iam workload-identity-pools create "github" \
  --project="mrmemento" \
  --location="global" \
  --display-name="GitHub Actions Pool"
```

#### extract pool full ID

```
gcloud iam workload-identity-pools describe "github" \
  --project="mrmemento" \
  --location="global" \
  --format="value(name)"

```

Full ID from above --> `projects/445183189637/locations/global/workloadIdentityPools/github`

#### create provider

```
gcloud iam workload-identity-pools providers create-oidc "crosswords-dot-svg" \
  --project="mrmemento" \
  --location="global" \
  --workload-identity-pool="github" \
  --display-name="My GitHub repo Provider" \
  --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
  --attribute-condition="assertion.repository_owner == 'ppmclala'" \
  --issuer-uri="https://token.actions.githubusercontent.com"
```

#### extract provider resource name

```
gcloud iam workload-identity-pools providers describe "crosswords-dot-svg" \
  --project="mrmemento" \
  --location="global" \
  --workload-identity-pool="github" \
  --format="value(name)"
```

resource name --> `projects/445183189637/locations/global/workloadIdentityPools/github/providers/crosswords-dot-svg`

^^^ use this value in the GH actions YAML

#### allow authentications from this pool to Google Cloud

gcloud secrets add-iam-policy-binding "my-secret" \
  --project="mrmemento" \
  --role="roles/secretmanager.secretAccessor" \
  --member="principalSet://iam.googleapis.com/projects/445183189637/locations/global/workloadIdentityPools/github/attribute.repository/ppmclala/crosswords.svg"

#### using google application credentials instead of service account

https://cloud.google.com/docs/authentication/provide-credentials-adc#how-to

##### local dev

```
$ gcloud auth application-default login
```
Sticks creds into `~/.config/gcloud/application_default_credentials.json`

grab the ID_TOKEN from ADC (s/ID/ACCESS/ig to get the ACCESS_TOKEN)

```
$ ID_TOKEN=`curl --silent --request POST 'https://oauth2.googleapis.com/token ' --header 'Content-Type: application/json' --data-raw "$(jq '. | .grant_type= "refresh_token" '  ~/.config/gcloud/application_default_credentials.json)" | bb -i --eval '(-> (str/join *input*) json/parse-string (get "id_token") println)'`
```

add scopes to ADC

```
gcloud auth application-default login --scopes=openid,https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/drive,https://www.googleapis.com/auth/drive.file,https://www.googleapis.com/auth/drive.readonly,https://www.googleapis.com/auth/spreadsheets,https://www.googleapis.com/auth/spreadsheets.readonly
```

#### auth setup

* local: use cli to getn application default creds
* GH Actions: use workload identity pools to connect
* mrmemento: use service account to get application default creds

#### enable service account credentials API on gcloud 

https://console.cloud.google.com/apis/api/iamcredentials.googleapis.com/metrics?project=mrmemento

add roles to SA
* SA OIDC ID token creator
* SA token creator
* Workload Identity User


##### env prep

* do we need an SA for the Direct model?
* define attribute mappings:
  * sub
  * repo_id
  * repo_owner_id
* define attr conditions
  * assertion.repo_id == 123 && assertion.ref = 'refs/heads/main'

  ##### take two w/ fresh SA

* reference

https://cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines#gcloud

1. defin attr mapping and condition

```
  --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
  --attribute-condition="assertion.repository_owner == 'ppmclala'" \
```
2. pool id: 'github'
3. project name: 'mrmemento'
4. provider id: 'crosswords-dot-svg'
5. new SA: crosswords-svg@mrmemento.iam.gserviceaccount.com
6. project number: 445183189637
6. GH subject: repo:ppmclala/crosswords.svg:ref:refs/heads/main
7. grant workload identity user role

```
gcloud iam service-accounts add-iam-policy-binding crosswords-svg@mrmemento.iam.gserviceaccount.com \
    --role=roles/iam.workloadIdentityUser \
    --member="principal://iam.googleapis.com/projects/445183189637/locations/global/workloadIdentityPools/github/subject/repo:ppmclala/crosswords.svg:ref:refs/heads/main"
```
8. control scopes for Access Token?

* add token_format = 'access_token'
* just use ScopedCredentials
