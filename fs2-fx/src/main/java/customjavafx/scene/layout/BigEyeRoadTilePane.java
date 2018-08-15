package customjavafx.scene.layout;


import customjavafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.layout.TilePane;

public class BigEyeRoadTilePane extends TilePane {

    private int column = 0;
    private int row = -1;
    private int savedColumn = -1;

    public BigEyeRoadTilePane() {
        super();
        super.setOrientation(Orientation.VERTICAL);
        super.setAlignment(Pos.TOP_LEFT);
    }

    private int getSizeLimit() {
        return (getPrefColumns() * getPrefRows());
    }

    private boolean childrenLimitReached() {
        return (getChildren().size() == getSizeLimit());
    }

    private void Insert() {
        this.getChildren().add(new BigEyeRoadLabel(BigEyeRoadResult.EMPTY));
    }

    private int getSize() {
        return getCurrentPosition() + 1;
    }

    private int getCurrentPosition() {
        return (column * getPrefRows()) + row;
    }

    private void ClearLabel(BigEyeRoadLabel t) {
        t.setResult(BigEyeRoadResult.EMPTY);
    }

    private boolean isCurrentWinRed() {
        switch (((BigEyeRoadLabel) super.getChildren().get(getCurrentPosition())).getResult()) {
            case RED:
                return true;
            default:
                return false;
        }
    }

    private boolean isCurrentWinBlue() {
        switch (((BigEyeRoadLabel) super.getChildren().get(getCurrentPosition())).getResult()) {
            case BLUE:
                return true;
            default:
                return false;
        }
    }

    private boolean isNextWinRed(BigEyeRoadResult win) {
        switch (win) {
            case RED:
                return true;
            default:
                return false;
        }
    }
    private boolean isNextWinBlue(BigEyeRoadResult win) {
        switch (win) {
            case BLUE:
                return true;
            default:
                return false;
        }
    }

    private void ShiftColumnNew() {
        int localSaveRow= row;
        int localSaveColumn = column;
        this.getChildren().remove(0, (getPrefRows()));
        for(int i=0; i < (getPrefRows());i++){
            Insert();
        }
        row = localSaveRow;
        column= localSaveColumn - 1;
        if(savedColumn != -1) savedColumn--;
    }


    private void MoveForSameColor() {
        if ((getPrefRows() - (row + 1)) == 0) {
            if(savedColumn == -1) savedColumn = column;
            column++;

        } else {
            if (((BigEyeRoadLabel) super.getChildren().get(getCurrentPosition() + 1)).getResult() == BigEyeRoadResult.EMPTY){
                row++;
            }
            else {
                if(savedColumn == -1) savedColumn = column;
                column++;

            }
        }
    }

    private void MoveForDifferentColor() {
        if(savedColumn != -1) {
            column = savedColumn + 1;
            savedColumn = -1;
            row = 0;
        }
        else {
            column++;
            row = 0;

        }
    }


    private void MoveToNextPosition(BigEyeRoadResult next) {
        if (getSize() != 0) {
            if (isCurrentWinRed()) {
                if (isNextWinRed(next)) {
                    MoveForSameColor();
                } else {
                    if (isNextWinBlue(next)) {
                        MoveForDifferentColor();
                    }
                }
            }
            else if (isCurrentWinBlue()) {
                if (isNextWinRed(next)) {
                    MoveForDifferentColor();
                } else {
                    if (isNextWinBlue(next)) {
                        MoveForSameColor();
                    }
                }
            }
        } else {
            row++;
        }
        if (getCurrentPosition() >= getSizeLimit()) {
            ShiftColumnNew();

        }
    }

    private void AddElement(BigEyeRoadResult next) {
        MoveToNextPosition(next);
        ((BigEyeRoadLabel)getChildren().get(getCurrentPosition())).setResult(next);
    }

    public void ReArrangeElements(ObservableList<BigEyeRoadLabel> labels) {
        getChildren().stream().map(x -> (BigEyeRoadLabel)x ).forEach(t-> ClearLabel(t));
        row = -1;column = 0;savedColumn= -1;
        labels.stream().forEach(t-> {
            AddElement(t.getResult());
        });
    }

    public void Initialize(int row, int column) {
        this.setPrefRows(row);
        this.setPrefColumns(column);
        while (!childrenLimitReached()) {
            Insert();
        }
        this.column = 0;
        this.row = -1;
    }

    public void Reset(){
        getChildren().stream().map(t->(BigEyeRoadLabel)t).forEach(t->{
            t.setResult(BigEyeRoadResult.EMPTY);
        });
        column = 0;
        row= -1;
        savedColumn = -1;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
    }


}
