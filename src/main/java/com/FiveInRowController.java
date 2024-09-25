package com;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class FiveInRowController {
    /**
     * 控件
     */
    public Button goBack;
    public AnchorPane chessBoardUi;
    public GridPane chessBoard;
    public Button regret;
    public Button reStart;
    public Circle blackCircle;
    public Circle whiteCircle;
    public Label blackLabelTimer;
    public Label whiteLabelTimer;

    /**
     * 辅助属性
     */
    enum Mode {RobotMode, DoubleMode, SingleMode;}

    Set<String> chessAround = new HashSet<>();//已放置棋子的周围部分,用于人机模式的机器下棋
    static Mode Pattern = Mode.DoubleMode;
    Color playerColor = Color.BLACK;

    enum State {Running, End}

    ;

    enum GuessFirst {First, Last}

    ;
    static GuessFirst guessFirst = GuessFirst.First;

    int radius = 15;
    int BoardLen = 19;
    State state = State.Running;
    Stack<Circle> circles = new Stack<>();

    Stack<int[]> circlesPos = new Stack<>();

    int[][] chessBoardArray = new int[19][19];
    private Circle preCircle;


    /**
     * 初始化
     */
    public void initialize() {
//        System.out.println("initialize");
        state = State.Running;
        playerColor = Color.BLACK;
        whiteCircle.setStroke(null);
        blackCircle.setStroke(Color.RED);
        if (Pattern == Mode.RobotMode) playerChoose();
    }

    /**
     * 基础落子和判断是否胜利
     */
    public void onMouseClicked(MouseEvent mouseEvent) {
        /*
           落子  判赢 改变棋子颜色 下一步
         */
        if (state == State.End) return;
        int col = (int) ((int) mouseEvent.getX() / (chessBoard.getWidth() / 19));
        int row = (int) ((int) mouseEvent.getY() / (chessBoard.getHeight() / 19));
        if (col > 18 || row > 18 || col < 0 || row < 0 || chessBoardArray[row][col] != 0) return;
        putChess(row, col);
        if (Pattern == Mode.RobotMode) {
            robotMove(row, col);
        }
    }

    void putChess(int row, int col) {
        if (chessBoardArray[row][col] != 0) return;
        chessBoardArray[row][col] = playerColor == Color.BLACK ? 1 : -1;
        Circle circle = new Circle(0, 0, radius, playerColor);
        circles.push(circle);
        circlesPos.push(new int[]{row, col});
        chessBoard.add(circle, col, row);
        chessBoardArray[row][col] = ((playerColor == Color.BLACK) ? 1 : -1);
        chessAround.remove(row + "-" + col);//存储周围
        for (int[] dir : direction) {
            int ni = row + dir[0], nj = col + dir[1];
            if (!isValidIndex(ni, nj)) continue;
            chessAround.add(ni + "-" + nj);
        }
        judge((playerColor == Color.BLACK ? 1 : -1), row, col);
        playerColor = playerColor == Color.BLACK ? Color.WHITE : Color.BLACK;
        if (Pattern != Mode.SingleMode) addTimer();
        if (playerColor == Color.BLACK) {
            blackCircle.setStroke(Color.RED);
            whiteCircle.setStroke(null);
        } else {
            blackCircle.setStroke(null);
            whiteCircle.setStroke(Color.RED);
        }

    }

    /**
     * 判断有没有赢,并且弹出窗口
     */

    private boolean isVictory(int value, int i, int j) {
        //横向连5个以上,或者纵向连5个以上,或者左斜连5个以上,或者右斜连5个以上
        if (countRow(value, i, j).count >= 5 || countCol(value, i, j).count >= 5) return true;
        return countMainDiagonal(value, i, j).count >= 5 || countSubDiagonal(value, i, j).count >= 5;
    }

    private void judge(int value, int row, int col) {
        if (isVictory(value, row, col)) {
            winAlert();
            state = State.End;
        }
    }

    private void winAlert() {
        String text = "恭喜" + (playerColor == Color.BLACK ? "黑" : "白") + "棋玩家获胜！";
        Alert victoryAlert = new Alert(Alert.AlertType.INFORMATION);
        victoryAlert.setTitle("游戏结束");
        victoryAlert.setHeaderText("胜利！");
        victoryAlert.setContentText(text);
        victoryAlert.show();
    }

    /**
     * 预先显示棋子落点，鼠标离开时消失
     */
    public void onMouseMoved(MouseEvent mouseEvent) {
        chessBoard.getChildren().remove(preCircle);
        int col = (int) ((int) mouseEvent.getX() / (chessBoard.getWidth() / 19));
        int row = (int) ((int) mouseEvent.getY() / (chessBoard.getHeight() / 19));
        if (col > 18 || row > 18 || col < 0 || row < 0) return;
        if (chessBoardArray[row][col] != 0) return;
        preCircle = new Circle(0, 0, radius, playerColor == Color.BLACK ? new Color(0, 0, 0, 0.5) : new Color(1, 1, 1, 0.5));
        chessBoard.add(preCircle, col, row);
    }

    public void onMouseExited() {
        chessBoard.getChildren().remove(preCircle);
    }

    int stepTime = 31;//每步的时间
    int remainderTime;//当前玩家剩余的时间

    Thread threadTimer;

    /**
     * 添加倒计时线程
     */
    private void addTimer() {
        if (threadTimer != null) threadTimer.interrupt();//中断上次的线程
        remainderTime = stepTime;
        //开启新线程
        threadTimer = new Thread(() -> {
            while (state != State.End) {
                remainderTime--;
                Platform.runLater(() -> {
                    (playerColor == Color.BLACK ? blackLabelTimer : whiteLabelTimer).setText(remainderTime + "");
                    (playerColor == Color.BLACK ? whiteLabelTimer : blackLabelTimer).setText(null);
                    if (remainderTime == 0) {//当前玩家花完了全部时间,输
                        timeOutAlert();
                        state = State.End;//线程终止
                    }
                });
                try {
                    Thread.sleep(1000);//每次减1秒
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        threadTimer.start();
    }

    private void timeOutAlert() {
        String text = (playerColor == Color.BLACK ? "黑" : "白") + "棋超时,恭喜" + (playerColor == Color.BLACK ? "白" : "黑") + "玩家获胜！";
        Alert victoryAlert = new Alert(Alert.AlertType.INFORMATION);
        victoryAlert.setTitle("游戏结束");
        victoryAlert.setHeaderText((playerColor == Color.BLACK ? "黑" : "白") + "棋超时！");
        victoryAlert.setContentText(text);
        victoryAlert.show();
    }

    private void playerChoose() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("选择先后手");
        alert.setHeaderText(null);
        ButtonType op1 = new ButtonType("先手");
        ButtonType op2 = new ButtonType("后手");
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(op1, op2);
        Optional<ButtonType> result = alert.showAndWait();
        ButtonType type = result.get();
        if (type == op2) {//玩家选择后手,机器先下一步
            putChess(BoardLen / 2, BoardLen / 2);
        }
    }

    /**
     * 机器算法下棋
     */
    private void robotMove(int playerI, int playerJ) {
        int[] result = getPos(playerI, playerJ);
        int i = result[0], j = result[1];
        putChess(i, j);
    }

    private boolean isValidIndex(int i, int j) {
        return 0 <= i && i <= BoardLen && 0 <= j && j <= BoardLen && chessBoardArray[i][j] == 0;
    }

    private int[] getPos(int playerI, int playerJ) {
        int i = -1, j = -1;//最终放置位置
        //检查玩家上次放置的位置,横向,纵向,斜向的有多少个连接
        Counter cntRow = countRow(playerColor == Color.BLACK ? 1 : -1, playerI, playerJ);
        Counter cntCol = countCol(playerColor == Color.BLACK ? 1 : -1, playerI, playerJ);
        Counter cntMain = countMainDiagonal(playerColor == Color.BLACK ? 1 : -1, playerI, playerJ);
        Counter cntSub = countSubDiagonal(playerColor == Color.BLACK ? 1 : -1, playerI, playerJ);
        if (cntRow.count >= 3) {//有三个或以上,往两边堵
            //检查两端是否有棋子,没有棋子就下
            if (isValidIndex(cntRow.x1, cntRow.y1 - 1) && chessBoardArray[cntRow.x1][cntRow.y1 - 1] == 0) {
                i = cntRow.x1;
                j = cntRow.y1 - 1;
            } else if (isValidIndex(cntRow.x2, cntRow.y2 + 1) && chessBoardArray[cntRow.x2][cntRow.y2 + 1] == 0) {
                i = cntRow.x2;
                j = cntRow.y2 + 1;
            }
        } else if (cntCol.count >= 3) {
            if (isValidIndex(cntRow.x1 - 1, cntRow.y1) && chessBoardArray[cntCol.x1 - 1][cntCol.y1] == 0) {
                i = cntCol.x1 - 1;
                j = cntCol.y1;
            } else if (isValidIndex(cntRow.x2 + 1, cntRow.y2) && chessBoardArray[cntCol.x2 + 1][cntCol.y2] == 0) {
                i = cntCol.x2 + 1;
                j = cntCol.y2;
            }
        } else if (cntMain.count >= 3) {
            if (isValidIndex(cntRow.x1 - 1, cntRow.y1 - 1) && chessBoardArray[cntMain.x1 - 1][cntMain.y1 - 1] == 0) {
                i = cntMain.x1 - 1;
                j = cntMain.y1 - 1;
            } else if (isValidIndex(cntRow.x2 + 1, cntRow.y2 + 1) && chessBoardArray[cntMain.x2 + 1][cntMain.y2 + 1] == 0) {
                i = cntMain.x2 + 1;
                j = cntMain.y2 + 1;
            }
        } else if (cntSub.count >= 3) {
            if (chessBoardArray[cntSub.x1 + 1][cntSub.y1 - 1] == 0) {
                i = cntSub.x1 + 1;
                j = cntSub.y1 - 1;
            } else if (isValidIndex(cntRow.x2 - 1, cntRow.y2 + 1) && chessBoardArray[cntSub.x2 - 1][cntSub.y2 + 1] == 0) {
                i = cntSub.x2 - 1;
                j = cntSub.y2 + 1;
            }
        }
        if (!isValidIndex(i, j)) {
            //随机生成
            String[] around = chessAround.toArray(new String[0]);
            do {
                Random random = new Random();
                int k = random.nextInt(0, around.length);
                String[] id = (around[k]).split("-");
                i = Integer.parseInt(id[0]);
                j = Integer.parseInt(id[1]);
            } while (chessBoardArray[i][j] != 0);
        }
        return new int[]{i, j};
    }


    /**
     * 用于计算棋子连接个数的内部类
     */
    static class Counter {
        int count;//棋子连接的个数
        int x1, y1, x2, y2;//棋子连接的两端坐标(含),(x1,y1)偏左上角,(x2,y2)偏右下角

        public Counter(int count, int x1, int y1, int x2, int y2) {
            this.count = count;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public String toString() {
            return "Counter{count=" + count + ", x1=" + x1 + ", y1=" + y1 + ", x2=" + x2 + ", y2=" + y2 + '}';
        }
    }

    int[][] direction = new int[][]{{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private Counter countRow(int match, int i, int j) {
        int count = 1;
        int x1, y1, x2, y2;
        int k;
        for (k = j - 1; k >= 0 && chessBoardArray[i][k] == match; k--) {
            count++;
        }
        x1 = i;
        y1 = k + 1;
        int BoardLen = 19;
        for (k = j + 1; k <= BoardLen && chessBoardArray[i][k] == match; k++) {
            count++;
        }
        x2 = i;
        y2 = k - 1;
        return new Counter(count, x1, y1, x2, y2);
    }

    private Counter countCol(int match, int i, int j) {
        int count = 1;
        int x1, y1, x2, y2;
        int k;
        for (k = i - 1; k >= 0 && chessBoardArray[k][j] == match; k--) {
            count++;
        }
        x1 = k + 1;
        y1 = j;
        for (k = i + 1; k <= BoardLen && chessBoardArray[k][j] == match; k++) {
            count++;
        }
        x2 = k - 1;
        y2 = j;
        return new Counter(count, x1, y1, x2, y2);
    }

    private Counter countMainDiagonal(int match, int i, int j) {
        int count = 1;
        int x1, y1, x2, y2;
        int k;
        for (k = -1; i + k >= 0 && j + k >= 0 && chessBoardArray[i + k][j + k] == match; k--) {
            count++;
        }
        x1 = i + k + 1;
        y1 = j + k + 1;
        for (k = 1; i + k <= BoardLen && j + k <= BoardLen && chessBoardArray[i + k][j + k] == match; k++) {
            count++;
        }
        x2 = i + k - 1;
        y2 = j + k - 1;
        return new Counter(count, x1, y1, x2, y2);
    }

    private Counter countSubDiagonal(int match, int i, int j) {
        int count = 1;
        int x1, y1, x2, y2;
        int k;
        for (k = 1; i + k <= BoardLen && j - k >= 0 && chessBoardArray[i + k][j - k] == match; k++) {
            count++;
        }
        x1 = i + k - 1;
        y1 = j - k + 1;
        for (k = 1; i - k >= 0 && j + k <= BoardLen && chessBoardArray[i - k][j + k] == match; k++) {
            count++;
        }
        x2 = i - k + 1;
        y2 = j + k - 1;
        return new Counter(count, x1, y1, x2, y2);
    }

    /**
     * 悔棋
     */
    public void regret() {
        if (circles.empty()) return;
        state = State.Running;
        chessBoard.getChildren().remove(circles.pop());
        int[] arr = circlesPos.pop();
        chessBoardArray[arr[0]][arr[1]] = 0;
        playerColor = playerColor == Color.BLACK ? Color.WHITE : Color.BLACK;
        (playerColor == Color.BLACK ? blackCircle : whiteCircle).setStroke(Color.RED);
        (playerColor == Color.BLACK ? whiteCircle : blackCircle).setStroke(null);
        if (Pattern != Mode.SingleMode) addTimer();
    }

    /**
     * 重新开始
     */
    public void reStart() {
        chessBoard.getChildren().clear();
        chessBoardArray = new int[19][19];
        circlesPos = new Stack<>();
        circles = new Stack<>();
        if (threadTimer != null) threadTimer.interrupt();
        whiteLabelTimer.setText(null);
        blackLabelTimer.setText(null);
        initialize();
    }

    /**
     * 返回主界面
     */
    public void goBack() {
        if (threadTimer != null) threadTimer.interrupt();
        try {
            // 加载第二个界面的FXML文件
            FXMLLoader loader = new FXMLLoader(Start.class.getResource("Start.fxml"));
            Parent secondSceneRoot = loader.load();
            // 创建一个新的场景并将其设置为当前舞台的场景
            Scene secondScene = new Scene(secondSceneRoot);
            Stage currentStage = (Stage) goBack.getScene().getWindow();
            currentStage.setScene(secondScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
