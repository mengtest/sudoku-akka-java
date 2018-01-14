package sudoku;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

import java.util.ArrayList;
import java.util.List;

class CellUnassignedActor extends AbstractLoggingActor {
    private final int row;
    private final int col;
    private final List<Integer> possibleValues;
    private final int boxIndex;

    private CellUnassignedActor(int row, int col) {
        this.row = row;
        this.col = col;
        boxIndex = boxFor(row, col);

        possibleValues = new ArrayList<>();

        for (int value = 1; value < 10; value++) {
            possibleValues.add(value);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SetCell.class, this::setCell)
                .match(Board.CloneUnassigned.class, this::cloneUnassigned)
                .build();
    }

    private void setCell(SetCell setCell) {
//        List<Integer> pv = new ArrayList<>(possibleValues);
//        log().debug("{} {}", setCell, possibleValues);

        if (isSameCell(setCell)) {
            cellSetByBoardRowColOrBox();
        } else if (isSameRowColOrBox(setCell)) {
            trimPossibleValues(setCell);
        }

        checkPossibleValues(setCell);

//        if (possibleValues.size() != pv.size()) {
//            String msg = String.format("%s trimmed (%d)%s -> (%d)%s", setCell, pv.size(), pv, possibleValues.size(), possibleValues);
//            log().debug("{}", msg);
//        }
    }

    private boolean isSameCell(SetCell setCell) {
        return setCell.row == this.row && setCell.col == this.col;
    }

    private boolean isSameRowColOrBox(SetCell setCell) {
        return isSameRow(setCell) || isSameCol(setCell) || isSameBox(setCell);
    }

    private boolean isSameRow(SetCell setCell) {
        return setCell.row == this.row;
    }

    private boolean isSameCol(SetCell setCell) {
        return setCell.col == this.col;
    }

    private boolean isSameBox(SetCell setCell) {
        return boxIndex == boxFor(setCell.row, setCell.col);
    }

    private int boxFor(int row, int col) {
        int boxRow = (row - 1) / 3 + 1;
        int boxCol = (col - 1) / 3 + 1;
        return (boxRow - 1) * 3 + boxCol;
    }

    private void trimPossibleValues(SetCell setCell) {
        possibleValues.removeIf(value -> value == setCell.value);
    }

    private void checkPossibleValues(SetCell setCell) {
        if (possibleValues.size() == 1) {
            cellSetByThisCell();
        } else if (possibleValues.isEmpty()) {
            cellIsInvalid();
        } else {
            getContext().getParent().tell(new CellState.NoChange(setCell), getSelf());
        }
    }

    private void cellSetByBoardRowColOrBox() {
        getContext().stop(getSelf());
    }

    private void cellSetByThisCell() {
        String who = String.format("Set by cell (%d, %d) = %d", row, col, possibleValues.get(0));
        getSender().tell(new SetCell(row, col, possibleValues.get(0), who), getSelf());
        getContext().stop(getSelf());
    }

    private void cellIsInvalid() {
        getSender().tell(new CellState.Invalid(row, col), getSelf());
    }

    private void cloneUnassigned(Board.CloneUnassigned cloneUnassigned) {
        cloneUnassigned.boardClone.tell(new CellState.CloneUnassigned(row, col, possibleValues, cloneUnassigned.boardStalled, cloneUnassigned.boardClone), getSelf());
    }

    static Props props(int row, int col) {
        return Props.create(CellUnassignedActor.class, () -> new CellUnassignedActor(row, col));
    }
}
