import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Board extends JFrame {
    private int[][] bool = new int[21][21];// =0空的=1有黑棋=2有白棋
    int count = 0;//count按下換棋按鈕後選取的點的數量
    int winner = 0;// =1黑贏=2白贏
    int test=1;//=1代表黑方 0白方
    int tempi,tempj;
    private boolean BtPress;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Board frame = new Board();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public Board() {
        for (int i = 0; i <= 20; i++)
            for (int j = 0; j <= 20; j++)
                bool[i][j] = 0;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        setContentPane(panel);
        setSize(800, 800);
        Rectangle[][] chessArray = new Rectangle[21][21];
        for (int i = 0; i <= 20; i++)
            for (int j = 0; j <= 20; j++)
                chessArray[i][j] = new Rectangle(25 * j, 25 * i, 20, 20);// 棋盤位置為中心 邊長20的正方形
        JPanel chessPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.DARK_GRAY);
                for (int i = 0; i <= 20; i++) {
                    g.drawLine(10 + 25 * i, 10, 10 + 25 * i, 510);
                    g.drawLine(10, 10 + 25 * i, 510, 10 + 25 * i);
                }
                for (int i = 0; i <= 20; i++)// repaint時讀bool的值決定塗黑or白
                    for (int j = 0; j <= 20; j++) {
                        if (bool[i][j] == 1)
                            drawBlackCircle(g, chessArray[i][j].x + 10, chessArray[i][j].y + 10, 10);
                        else if (bool[i][j] == 2)
                            drawWhiteCircle(g, chessArray[i][j].x + 10, chessArray[i][j].y + 10, 10);
                    }

            }
        };
        chessPanel.setBounds(178, 5, 520, 520);
        chessPanel.setPreferredSize(new Dimension(520, 520));

        JLabel Blackfun = new JLabel("黑方");
        Blackfun.setFont(new Font("新細明體", Font.PLAIN, 20));
        Blackfun.setBounds(133, 253, 40, 24);

        JLabel Whitefun = new JLabel("白方");
        Whitefun.setFont(new Font("新細明體", Font.PLAIN, 20));
        Whitefun.setBounds(703, 253, 40, 24);
        panel.setLayout(null);
        
        JButton Sacrified = new JButton("換棋");
        Sacrified.setFont(new Font("新細明體", Font.PLAIN, 20));
        Sacrified.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		count=0;
        		BtPress=true;
        	}
        });
        Sacrified.setBounds(10, 10, 90, 30);
        panel.add(Sacrified);
        getContentPane().add(Blackfun);
        getContentPane().add(chessPanel);
        getContentPane().add(Whitefun);
        
        JButton decide = new JButton("確定");
        decide.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		if(count==2) {
        			JOptionPane.showMessageDialog(null,"請選擇一顆對方的棋");
        			count++;
        		}
        			
        	}
        });
        decide.setFont(new Font("新細明體", Font.PLAIN, 20));
        decide.setBounds(10, 53, 90, 30);
        panel.add(decide);
        chessPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int newi = (int) Math.ceil(e.getY() / 25.f);
                int newj = (int) Math.ceil(e.getX() / 25.f);
                if (chessArray[newi - 1][newj - 1].contains(e.getPoint())) {
                    newi -= 1;
                    newj -= 1;
                } else if (chessArray[newi - 1][newj].contains(e.getPoint()))
                    newi -= 1;
                else if (chessArray[newi][newj - 1].contains(e.getPoint()))
                    newj -= 1;
                else if (!chessArray[newi][newj].contains(e.getPoint()))
                    return;
                if(BtPress!=false&&count<2){//換棋且還未選到兩顆
                	if(test==1&&bool[newi][newj]==1) {
                		if(count==0) {
                			tempi=newi;
                			tempj=newj;
                		}
                		else if(count==1&&bool[tempi][tempj]==1) {
                			bool[newi][newj]=0;
                			bool[tempi][tempj]=0;
                		}
                			
                		count++;
                	}
                	else if(test==2&&bool[newi][newj]==2) {
                		if(count==0) {
                			tempi=newi;
                			tempj=newj;
                		}
                		else if(count==1&&bool[tempi][tempj]==2) {
                			bool[newi][newj]=0;
                			bool[tempi][tempj]=0;
                		}
                			
                		count++;
                	}
                	else
                		count=0;
                	//BtPress=true;
                	return;
                }
                else if(count==3) {
                	if(test==1&&bool[newi][newj]==2) {
                		bool[newi][newj]=1;
                		count++;
                		test=2;
                		repaint();
                	}
                	else if(test==2&&bool[newi][newj]==1) {
                		bool[newi][newj]=2;
                		count++;
                		test=1;
                		repaint();
                	}
                }
                else{
                	if (bool[newi][newj] == 0) {
                		if(test==1) {
                       // if (isBlack()) {
                            bool[newi][newj] = 1;
                            determine(newi, newj, 1);
                            test=2;
                        } else {
                            bool[newi][newj] = 2;
                            determine(newi, newj, 2);
                            test=1;
                        }
                        repaint();
                    }
                }
                
                if (winner == 1)
                    JOptionPane.showMessageDialog(null, "Black Win");
                else if (winner == 2)
                    JOptionPane.showMessageDialog(null, "White Win");
            }
        });

    }

    public boolean isBlack() {
        return false;
    }

    public void determine(int row, int col, int color) {
        Point currentPoint = new Point(row, col);
        int up = countChess(currentPoint, new Point(0, -1));
        int down = countChess(currentPoint, new Point(0, 1));
        int left = countChess(currentPoint, new Point(-1, 0));
        int right = countChess(currentPoint, new Point(1, 0));
        int up_left = countChess(currentPoint, new Point(-1, -1));
        int up_right = countChess(currentPoint, new Point(1, -1));
        int down_left = countChess(currentPoint, new Point(-1, 1));
        int down_right = countChess(currentPoint, new Point(1, 1));
        if (up + down == 4 || left + right == 4 || up_left + down_right == 4 || up_right + down_left == 4) {
            if (color == 1)
                winner = 1;
            else if (color == 2)
                winner = 2;
        }

    }

    public int countChess(Point currentPoint, Point vector) {
        Point nextPoint = new Point(currentPoint.x + vector.x, currentPoint.y + vector.y);
        if (nextPoint.x > 20 || nextPoint.x < 0 || nextPoint.y > 20 || nextPoint.y < 0) {
            return 0;
        }
        if (bool[currentPoint.x][currentPoint.y] == bool[nextPoint.x][nextPoint.y]) {
            return 1 + countChess(nextPoint, vector);
        }
        return 0;
    }

    public static void drawBlackCircle(Graphics g, int x, int y, int radius) {
        int diameter = radius * 2;
        g.setColor(Color.BLACK);
        g.fillOval(x - radius, y - radius, diameter, diameter);
    }

    public static void drawWhiteCircle(Graphics g, int x, int y, int radius) {
        int diameter = radius * 2;
        g.setColor(Color.WHITE);
        g.fillOval(x - radius, y - radius, diameter, diameter);
    }
}