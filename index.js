const ACROSS = 0;
const DOWN = 1;
const state = {};

state.selectedPuzzle = 0;
state.selectedCell = { row: 0, col: 0 };

const setSelectedPuzzle = (e, _) => {
  state.selectedPuzzle = e.srcElement.value;
  d3.selectAll('g.clues').remove();
  d3.selectAll('#puzzle-selector').remove();
  d3.selectAll('svg').remove();
  puzzle();
};

const puzzle = () => {
  var board = [
    [1, 2, 3, 4, "*", 5, 6, 7, 8, 9, "*", 10, 11, 12, 13],
    [14, "_", "_", "_", "*", 15, "_", "_", "_", "_", "*", 16, "_", "_", "_"],
    [17, "_", "_", "_", "*", 18, "_", "_", "_", "_", "*", 19, "_", "_", "_"],
    [20, "_", "_", "*", 21, "_", "_", "_", "*", 22, 23, "_", "_", "_", "_"],
    ["*", "*", 24, 25, "_", "_", "_", "*", 26, "_", "_", "_", "_", "*", "*"],
    [27, 28, "_", "_", "_", "_", "*", 29, "_", "_", "_", "_", "*", 30, 31],
    [32, "_", "_", "_", "_", "*", 33, "_", "_", "_", "_", "*", 34, "_", "_"],
    [35, "_", "_", "_", "*", 36, "_", "_", "_", "_", "*", 37, "_", "_", "_"],
    [38, "_", "_", "*", 39, "_", "_", "_", "_", "*", 40, "_", "_", "_", "_"],
    [41, "_", "*", 42, "_", "_", "_", "_", "*", 43, "_", "_", "_", "_", "_"],
    ["*", "*", 44, "_", "_", "_", "_", "*", 45, "_", "_", "_", "_", "*", "*"],
    [46, 47, "_", "_", "_", "_", "*", 48, "_", "_", "_", "*", 49, 50, 51],
    [52, "_", "_", "_", "*", 53, 54, "_", "_", "_", "*", 55, "_", "_", "_"],
    [56, "_", "_", "_", "*", 57, "_", "_", "_", "_", "*", 58, "_", "_", "_"],
    [59, "_", "_", "_", "*", 60, "_", "_", "_", "_", "*", 61, "_", "_", "_"],
  ];

  var direction = ACROSS;

  const highlightSelected = () => {
    // clear old
    d3.selectAll("rect").classed("selected", false);
    d3.selectAll("text").classed("target", false);
    d3.selectAll("rect").classed("dirGuide", false);

    switch (direction) {
      case ACROSS:
        d3.selectAll(`g#r${state.selectedCell.row} rect.cell`).classed(
          "dirGuide",
          true
        );

        break;
      case DOWN:
        d3.selectAll(`rect#cell${state.selectedCell.col}.cell`).classed(
          "dirGuide",
          true
        );

        break;
    }

    // select the cell and mark target
    d3.select(
      `g#r${state.selectedCell.row} text#cell-text${state.selectedCell.col}`
    ).classed("target", true);

    d3.select(`g#r${state.selectedCell.row} rect#cell${state.selectedCell.col}`).classed(
      "selected",
      true
    );

    d3.select(`g#r${state.selectedCell.row} rect#cell${state.selectedCell.col}`).classed(
      "dirGuide",
      false
    );
  };

  const onCellClick = (e) => {
    e.preventDefault();

    state.selectedCell.row = Math.floor((e.y - 25) / cellWall);
    state.selectedCell.col = Math.floor((e.x - 30) / cellWall);

    highlightSelected();
  };

  const deselect = () => {
    state.selectedCell.col = null;
    state.selectedCell.row = null;
    highlightSelected();
  };

  const selectNextCol = (d) => {
    do {
      if (state.selectedCell.col === 0) state.selectedCell.col = board.length;
      state.selectedCell.col = (state.selectedCell.col + d) % board.length;
    } while (board[state.selectedCell.row][state.selectedCell.col] === "*");

    highlightSelected();
  };

  const selectNextRow = (d) => {
    do {
      if (state.selectedCell.row === 0) state.selectedCell.row = board.length;
      state.selectedCell.row = (state.selectedCell.row + d) % board.length;
    } while (board[state.selectedCell.row][state.selectedCell.col] === "*");

    highlightSelected();
  };

  const selectNextCell = (d) => {
    switch (direction) {
      case ACROSS:
        selectNextCol(d);
        break;
      case DOWN:
        selectNextRow(d);
        break;
    }
  };

  const onKeyDown = (e) => {
    var code = e.keyCode;
    if ((code >= 65 && code <= 90) || (code >= 97 && code <= 122)) {
      e.preventDefault();
      d3.selectAll(".target").text(e.key.toUpperCase());
      selectNextCell(1);
    } else {
      switch (code) {
        case 32:
          e.preventDefault();
          direction ^= 1;
          highlightSelected();
          break;
        case 37: // left
          e.preventDefault();
          selectNextCol(-1);
          break;
        case 38: // up
          e.preventDefault();
          selectNextRow(-1);
          break;
        case 39: // right
          e.preventDefault();
          selectNextCol(1);
          break;
        case 40:
          e.preventDefault();
          selectNextRow(1);
          break;

        case 13:
          e.preventDefault();
          deselect();
          break;

        case 46:
          e.preventDefault();
          d3.selectAll(".target").text("");
          break;
        case 8:
          e.preventDefault();
          d3.selectAll(".target").text("");
          selectNextCell(-1);
          break;
      }
    }
  };

  var body = d3.select("body").on("keydown", onKeyDown);


  var svg = body.append("svg").attr("width", 1200).attr("height", 1200);
  var cellWall = 50;
  var margin = 20;
  var row = svg
    .selectAll("g")
    .data(board)
    .enter()
    .append("g")
    .attr("transform", (_, i) => `translate(0,${i * cellWall + margin})`)
    .attr("id", (_, i) => `r${i}`);

  // cells
  row
    .selectAll("rect")
    .data((d) => d)
    .enter()
    .append("rect")
    .attr("id", (_, i) => `cell${i}`)
    .attr("class", (d) => (d === "*" ? "blocker" : "cell"))
    .attr("x", (_, j) => j * cellWall + margin)
    .attr("height", cellWall)
    .attr("width", cellWall);

  // clueNumbers
  row
    .selectAll("text")
    .data((d) => d)
    .enter()
    .append("text")
    .attr("class", "numbered")
    .attr("x", (_, j) => j * cellWall + margin + 2)
    .attr("y", 10)
    .text((d) => (!isNaN(d) ? d : ""));

  // inputs
  row
    .selectAll("text .inputText")
    .data((d) => d)
    .enter()
    .append("text")
    .attr("class", "inputText")
    .attr("x", (_, j) => j * cellWall + margin + 25)
    .attr("y", cellWall - 5)
    .attr("text-anchor", "middle")
    .attr("id", (_, i) => `cell-text${i}`)
    .text("");

  d3.selectAll("rect").on("click", onCellClick);
  d3.selectAll("inputText").on("click", onCellClick);

  // TODO: we should generate the dataset partitioned
  // by direction instead to avoid this re-mapping
  var clues = {
    across: puzzles[state.selectedPuzzle].clues
      .filter((d) => d.direction === "across")
      .map((d) => d.clue),
    down: puzzles[state.selectedPuzzle].clues
      .filter((d) => d.direction === "down")
      .map((d) => d.clue),
  };

  const addClues = (dir, x) => {
    svg
      .selectAll(`g.clues.${dir}`)
      .data(clues[dir].flatMap((c) => c))
      .enter()
      .append("g")
      .attr("transform", `translate(${x},0)`)
      .attr("class", `clues ${dir}`)
      .append("text")
      .attr("y", (_, i) => i * 20 + 35)
      .text((d) => d);
  };

  addClues("across", 800);
  addClues("down", 1000);

  body
    .append("select")
    .attr("id", "puzzle-selector")
    .selectAll("option")
    .data(puzzles)
    .enter()
    .append("option")
    .text((d) => d.name)
    .property('selected', (_, i) => i == state.selectedPuzzle ? "selected" : "")
    .attr("value", (_, i) => i);

  d3.select("select#puzzle-selector").on("change", setSelectedPuzzle);
};

window.onload = () => { puzzle(); }

