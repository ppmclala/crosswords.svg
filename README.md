# crosswords.svg

## what

A simple SVG [crossword puzzle](https://ppmclala.github.io/crosswords.svg) designed to be projected or displayed on large screens for use in group games.

## why

Replacement for a physical layout screen that was used by a memory care unit for a group activity.  Instead of re-printing a new copy, the idea is to use this project to catalog the puzzles from the accompanying book and have a simple mechanism for running group puzzle solving sessions. 

## how

Simplest thing possible with as few moving parts as possible.  App will be distributed as a single HTML file with d3.js code and game data structures baked in.  Data entry will be via a Google Sheets interface with a pre-processor to generate the data structures in the final package.

## license

This software is [unlicensed](./LICENSE).  Do with it what you please.

## development

* generate index.html for dev/test: `bb gen-index`
* build a single HTML file for release: `bb build`
* run an `nrepl` instance using babashka: `bb nrepl-server`
* sync data from source Google sheet into `gen/google-sync`: `bb sync`
* Github Action automates the [demo site](https://ppmclala.github.io/crosswords.svg) deployment to GH pages.  build pipeline: `bb clean && bb sync && bb build`

## thanks

* https://github.com/gstasiewicz/crossword - ideas on handling input
* https://bost.ocks.org/mike/ - for the great d3.js


