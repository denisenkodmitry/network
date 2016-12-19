import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author dmitry d
 */
public class Main extends JFrame implements ActionListener {

    private static final Integer[] ETALON_SECOND;
    private static final int RIGHTANGLE = 90;
    private static Thread threadGrafic;

    static {
        BufferedReader reader = getReaderFromFile("etalon.txt");
        String[] fileOut = new String[2];
        String[] strValues;
        Integer[] intValueSecond = new Integer[841];
        try {
            int i = 0;
            while (reader != null && reader.ready()) {
                fileOut[i] = reader.readLine();
                i++;
            }
        } catch (IOException e) {
            System.out.println("Что - то с загрузкой файла.");
        }

        strValues = fileOut[1].split(" ");
        for (int i = 0; i < strValues.length; i++) {
            intValueSecond[i] = Integer.valueOf(strValues[i]);
        }
        ETALON_SECOND = intValueSecond;
    }

    public Main() {
        super("MinimalStaticChart");
        createGUI();
    }

    public static void main(String[] args) {
        System.setProperty("jna.library.path", "C:\\Users\\superuser\\Downloads\\FANN-2.2.0-Source\\FANN-2.2.0-Source\\bin\\");

        System.out.println(System.getProperty("jna.library.path")); //maybe the path is malformed
        File file = new File(System.getProperty("jna.library.path") + "fannfloat.dll");
        System.out.println("Is the dll file there:" + file.exists());
        System.load(file.getAbsolutePath());

        final List<ArrayList<Integer>> listResult = new ArrayList<>();
        BufferedReader reader = getReaderFromFile("yes.txt");
        parseDataFromFile(listResult, reader, false);
        final Main frame = new Main();

        final ChartPanel chartPanel = new ChartPanel(null);

        threadGrafic = new ThreadGrafic(listResult, frame, chartPanel);

    }

    private static void parseDataFromFile(List<ArrayList<Integer>> listResult, BufferedReader reader, boolean test) {
        String line;
        String parts[];
        String tempParts[];
        Integer result[] = new Integer[841];
        // Это для рассчетов по теореме синусов
        int height;
        double sideA;
        double angleY;
        double angleB;
        Double offsetAngle = null;

        BufferedWriter writerYes = null;
        BufferedWriter writerNo = null;
        BufferedWriter writerSelect = null;
        try {
            if (test) {
                // Есть сляб или нет
                writerYes = new BufferedWriter(new FileWriter(new File("yes.txt")));
                writerNo = new BufferedWriter(new FileWriter(new File("no.txt")));
                writerSelect = new BufferedWriter(new FileWriter(new File("select.txt")));
            }
            while (reader != null && reader.ready()) {
                line = reader.readLine();
                if (line.contains("0\u0003\u0002sRA")) {
                    parts = line.split("0\u0003\u0002sRA");
                    for (String part : parts) {
                        tempParts = part.split(" ");
                        if (tempParts.length != 874) {
                            continue;
                        }
                        if (offsetAngle == null) {
                            offsetAngle = Integer.parseInt(tempParts[26], 16) / 10000d;
                        }
                        int indexResult = 0;
                        for (int j = 841; j > 0; j--) {
                            sideA = Integer.parseInt(tempParts[tempParts.length - 5 - j], 16);
                            if (indexResult < 420) {
                                angleY = 70 - offsetAngle * (indexResult + 1);
                            } else {
                                angleY = (offsetAngle * (indexResult + 1) - 70);
                            }
                            angleB = RIGHTANGLE - angleY;
                            height = (int) (sideA * Math.sin(Math.toRadians(angleB)) / Math.sin(Math.toRadians(RIGHTANGLE)));
                            result[indexResult++] = height;
                        }
                        listResult.add(new ArrayList<>(Arrays.asList(result)));
                    }
                }
                if (test && result[0] != null) {
                    if (detected(result)) {
                        writerYes.write(line);
                        writerYes.write("\r\n");
                        for (int i = 0; i < result.length; i++) {
                            writerSelect.write(String.valueOf(result[i]));
                            if (i < result.length - 1) {
                                writerSelect.write(" ");
                            }
                        }
                        //writerSelect.write("1");
                    } else {
                        writerNo.write(line);
                        writerNo.write("\r\n");
                        for (int i = 0; i < result.length; i++) {
                            writerSelect.write(String.valueOf(result[i]));
                            if (i < result.length - 1) {
                                writerSelect.write(" ");
                            }
                        }
                        //writerSelect.write("0");
                    }
                }
                writerSelect.write("\r\n");
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            System.out.println("wtf?!");
        } finally {
            try {
                writerYes.close();
                writerNo.close();
                writerSelect.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static BufferedReader getReaderFromFile(String name) {
        BufferedReader reader = null;
        try {
            File file = new File(name);
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            System.out.println("Нет такого файла");
        }
        return reader;
    }

    static void createChart(List<Integer> lineResult, JFrame frame, ChartPanel chartPanel) throws InterruptedException {
        XYSeries series1 = new XYSeries("serial1");

        for (int i = 0; i < lineResult.size(); i++) {
            series1.add(i, lineResult.get(i));
        }

        XYSeries series2 = new XYSeries("serial2");
        for (int i = 0; i < ETALON_SECOND.length; i++) {
            series2.add(i, ETALON_SECOND[i]);
        }

        XYSeriesCollection xyDataset = new XYSeriesCollection();
        xyDataset.addSeries(series1);
        xyDataset.addSeries(series2);
        JFreeChart chart = ChartFactory
                .createXYLineChart("График", "x", "y",
                        xyDataset,
                        PlotOrientation.VERTICAL,
                        true, true, true);

        // chartPanel.repaint();
        chartPanel.setChart(chart);
        frame.getContentPane()
                .add(chartPanel);

        Thread.sleep(100);

        SwingUtilities.updateComponentTreeUI(frame);
    }

    private static boolean detected(Integer[] result) {
        return searchItem(result, ETALON_SECOND);
    }

    private static boolean searchItem(Integer[] result, Integer[] etalon) {
        for (int i = 0; i < result.length; i += 30) {
            int resultCount = 0;
            for (int j = 0; j < 30; j++) {
                if ((i + j) < 841 && etalon[i + j] - result[i + j] > 30) {
                    resultCount++;
                }
            }
            if (resultCount > 15) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testDetected() {
        final List<ArrayList<Integer>> listResult = new ArrayList<>();
        BufferedReader reader = getReaderFromFile("test4.txt");
        parseDataFromFile(listResult, reader, true);
    }

    private void createGUI() {

        JMenuBar menubar = new JMenuBar();

        // создаем меню
        JMenu menu = new JMenu("Operations");

        // элемент 1
        JMenuItem itm = new JMenuItem("Start");
        menu.add(itm);
        itm.addActionListener(this);

        // элемент 2
        itm = new JMenuItem("Stop");
        itm.addActionListener(this);
        menu.add(itm);

        menubar.add(menu);
        setJMenuBar(menubar);

        setSize(600, 700);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("Start".equals(e.getActionCommand())) {
            if (Thread.State.WAITING.equals(threadGrafic.getState())) {
                threadGrafic.notify();
            }
        } else {
            threadGrafic.interrupt();
        }
    }
}

class ThreadGrafic extends Thread {

    private List<ArrayList<Integer>> listResult;
    private JFrame frame;
    private ChartPanel chartPanel;

    public ThreadGrafic() {

    }

    ThreadGrafic(List<ArrayList<Integer>> listResult, JFrame frame, ChartPanel chartPanel) {
        this.listResult = listResult;
        this.frame = frame;
        this.chartPanel = chartPanel;
        start();
    }

    @Override
    public void run() {
        try {
            if (!Thread.interrupted()) {
                for (ArrayList<Integer> aListResult : listResult) {
                    Main.createChart(aListResult, frame, chartPanel);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}