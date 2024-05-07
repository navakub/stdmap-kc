package standardmap;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimerTask;

import javax.swing.*;
import java.util.Objects;
import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
public class Graph extends JPanel implements ActionListener{
    protected double K;
    protected int N;
    
    //timer
    private java.util.Timer timer; private TimerTask task;
    private final int unit_time = 3000; 
            
    //for drawing
    Graphics2D g2D;

    //real value of map (Cartesian coordinate)
    private static ArrayList<ArrayList<Double[]>> curve_ArrayList;
    
    //value in screen (screen coordinate)
    public static ArrayList<ArrayList<Integer[]>> curve_plot_ArrayList;
    
            
    //coordinate transformation
    private static final double A1 = 2*Math.PI/(MainFrame.GRAPH_SIZE);
    private static final double B1 = 0 -A1*MainFrame.OFFSET_X;
    private static final double A2 = -2*Math.PI/(MainFrame.GRAPH_SIZE);
    private static final double B2 = 2*Math.PI - A2*MainFrame.OFFSET_Y;
    // --------------------------
    
    //boundary of graph
    private final int xmin = MainFrame.OFFSET_X;
    private final int xmax = MainFrame.GRAPH_SIZE + MainFrame.OFFSET_X - MainFrame.GRID_SIZE;
    private final int ymin = MainFrame.OFFSET_Y;
    private final int ymax = MainFrame.GRAPH_SIZE + MainFrame.OFFSET_Y - MainFrame.GRID_SIZE;
    
    //for algorithm
    //private ArrayList<Double> arrayList_K = new ArrayList<>(); int index; String state;
    //private ArrayList<String> arrayList_state = new ArrayList<>();
    private int numIter; //number of time used in approximation
    //private final double dx_c = 0.1;
    //private final double dy_c = 0.1;
    private int n; //number of "invariant curve" across the sides
    
    /*
    private final int num = 10;
    private final int numCurve = num*num; //number of curves in phase space
    private final double h = (double)(2*Math.PI/num); //step of initial conditions
    
    */
    private final double tol = 1E-10; //step of initial conditions
    private final int numCurve = 100; //number of curves in phase space
    private final double h = 2.*Math.PI/(double)numCurve; //step of initial conditions

    private int numChaos; //number of chaotic curve in "beyond" case, used for automatic estimation of Kc\
    private final double numPts = (Math.pow(((double)MainFrame.GRAPH_SIZE/(double)MainFrame.GRID_SIZE), 2)); //number of points in phase space if they are all filled

    //initial conditions (vary)
    public double U0, I0;
    
    public Double getK() {
        return K;
    }
    public int getN() {
        return N;
    }
    
    public void setK(double newK) {
        this.K = newK;
    }
    
    public void setN(int newN) {
        this.N = newN;
    }
    
    private void initializeArray(){
        curve_ArrayList = new ArrayList<>(); 
        curve_plot_ArrayList = new ArrayList<>();
    }
    
    public Graph(double K, int N) {
        this.K = K;
        this.N = N;
        initializeArray();
        
        JLabel titleGraph = new JLabel("Standard Map: Critical Value of K");
        add(titleGraph); 
    }
    
    
    //drawing Graph axis
    private void drawAxis(Graphics2D g2D){
        g2D.setStroke(new BasicStroke(3));
        g2D.drawRect(MainFrame.OFFSET_X, MainFrame.OFFSET_Y, MainFrame.GRAPH_SIZE, MainFrame.GRAPH_SIZE);
        
        int numX, numY; numX = 6; numY = 6;

        g2D.setStroke(new BasicStroke(1));
        for(int i=1; i<=numY; i++) g2D.drawLine(x2xs(0), y2ys((double)i), x2xs(2*Math.PI), y2ys((double)i)); //horizontal line 
        for(int i=1; i<=numX; i++) g2D.drawLine(x2xs((double)i), y2ys(0), x2xs((double)i), y2ys(2*Math.PI)); //vertical line
    }
    
    private void drawGraph(Graphics2D g2D){
        try{
        for (ArrayList<Integer[]> al : curve_plot_ArrayList){
            if(!al.isEmpty()){
                for (int j = 0; j < al.size(); j++) {
                    int Us = al.get(j)[0]; int Is = al.get(j)[1];
                    if(isInBound(Us,Is)) g2D.fillOval(Us, Is, MainFrame.GRID_SIZE, MainFrame.GRID_SIZE);
                }
            }
        }}catch(Exception e){
                System.out.println(e);
                }
    }
    
    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        g2D = (Graphics2D) g;
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawAxis(g2D);
        drawGraph(g2D);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }
    
    //stop calculation
    public void stop(){
        timer.cancel();
        repaint();
    }
    
    //clear graph here
    public void clearGraph(){
        initializeArray();
        repaint();
    }
    
    //generate graph here
    public void generateCurve(double U0, double I0, double K){
        ArrayList<Double[]> curve = invariantCurve(U0, I0, K); 
        ArrayList<Integer[]> curve_plot = invariantCurve_screen(curve);
        if(!curve_ArrayList.contains(curve)) curve_ArrayList.add(curve);
        if(!curve_plot_ArrayList.contains(curve_plot)) curve_plot_ArrayList.add(curve_plot);
    }
   
    // ---------------------------- ALGORITHM FOR Kc ----------------------------
    
    //function to generate graph from all possible initial conditions
    protected void generateCurves(double K) { 
        /*
        for(int i=0; i<num; i++) {
            for(int j=0; j<num; j++) { 
                generateCurve(i*h,j*h, K); 
            }
        }
        */
        for(int j=0; j<numCurve; j++) generateCurve(Math.PI,j*h, K); 
    }
    
   
    //function to generate graph from all possible initial conditions
    /*
    private void generateCurvesWithThreads(double K) throws InterruptedException { 
        Thread threads[][] = new Thread[num][num];
        for(int i=0; i<num; i++) {
            for(int j=0; j<num; j++) {
                final int ix = i; final int iy = j;
                Runnable r = () -> { generateCurve(ix*h, iy*h, K);};
                threads[i][j] = new Thread(r);
                threads[i][j].start();
            }
        }
        
        for(int i=0;i<num ; i++){
            for(int j=0;j<num ; j++){
                threads[i][j].join();
            }
        }
    }*/
    
    //sort the point from left to right. bottom to top
    private ArrayList<Double[]> sortCurve(ArrayList<Double[]> curve){
        ArrayList<Double[]> newArr = new ArrayList<>(); // sorted array
        
        ArrayList<Double> x = new ArrayList<>();
        for(int i=0; i<curve.size(); i++) x.add(curve.get(i)[0]);
        ArrayList<Double> y = new ArrayList<>();
        for(int i=0; i<curve.size(); i++) y.add(curve.get(i)[1]);
        
        //selection sort algorithm
        int pos;
        for (int i = 0; i < x.size(); i++){ 
            pos = i; 
            for (int j = i+1; j < x.size(); j++){
                if(x.get(j) < x.get(pos)) pos = j; //find the index of the minimum element
            }
            Collections.swap(x, i, pos);
            Collections.swap(y, i, pos);
        }
        
        int start = 0; boolean done = false;
        ArrayList<Integer> size = new ArrayList<>(); //size of unsorted y at the same x
        for (int i = 1; i < y.size(); i++){ 
            if(Objects.equals(x.get(i), x.get(i-1))) {
                if(!done){
                    done = true;
                    size.add(Collections.frequency(x, x.get(i)));
                }
            }else{
                done = false;
            }
        }
        
        for(int s: size){
            int unsorted = start + s;
            for (int i = start; i < unsorted; i++){ 
                pos = i; 
                for (int j = i+1; j < unsorted; j++){
                    if(y.get(j) < y.get(pos)) pos = j; //find the index of the minimum element
                }
                Collections.swap(y, i, pos);
            }
        }
        
        //return sorted array here
        for(int i=0; i<x.size(); i++){
            Double[] coor = {x.get(i), y.get(i)};
            newArr.add(coor);
        }
        return newArr;
    }
    
    private Double windingNumber(ArrayList<Double[]> curve){
        return (curve.get(getN()-1)[1])/getN();
    }

    private ArrayList<Double> windingNumber_ArrayList(ArrayList<ArrayList<Double[]>> curves){
        ArrayList<Double> ww = new ArrayList<>();
        for(ArrayList<Double[]> curve: curves){
            ww.add(windingNumber(curve));
        }
        return ww;
    }
    
    
    //check if the "sorted" curve is chaotic, only for "beyond" case
    private boolean isChaotic(){
        return numRegular(windingNumber_ArrayList(curve_ArrayList)) == 0;
    }
    
    //count iteration which is invariant curve in phase space
    private boolean isSpanned() {
        return !isChaotic();
    }
    
    private int numRegular(ArrayList<Double> ww){
        int numReg = 0;
        for(int j=1; j<numCurve-1; j++){
            if(ww.get(j+1) - ww.get(j) > tol && ww.get(j) - ww.get(j-1) > tol) numReg += 1;
        }
        return numReg;
    }
    
    private int numPeriodic(ArrayList<Double> ww){
        int numPer = 0;
        for(int j=1; j<numCurve-1; j++){
            if(Math.abs(ww.get(j+1) - ww.get(j)) > tol && Math.abs(ww.get(j-1) - ww.get(j)) > tol) numPer += 1;
        }
        return numPer;
    }
    
    //check the condition of K (below or beyond Kc)
    public void checkK(double K) {
        MainFrame.stateOfK = (isChaotic())? "beyond" : "below";
    }
    
    //update new K for automatic calculation
    public void updateK(){
        ArrayList<Double> ww = windingNumber_ArrayList(curve_ArrayList);
        if("below".equals(MainFrame.stateOfK)){
            double f1 = (double)numRegular(ww)/(double)numCurve;
            /*for(int i=0; i<numCurve; i++){
              System.out.println(ww.get(i));  
            }
            System.out.println("...");  */
            System.out.println("K:" + getK() + ", numPer: "+ numPeriodic(ww));  
            this.setK(this.K + f1);
        }else if("beyond".equals(MainFrame.stateOfK)){
            double f2 = 1.0 - (double)numPeriodic(ww)/(double)numCurve;
            this.setK(this.K - f2); 
        }
    }
    // numerically solve for Kc
    public synchronized void estimateKc() throws InterruptedException {
        numIter = 0;
        timer= new java.util.Timer();
        task = new TimerTask(){
            @Override
            public void run() {
                
                (MainFrame.labelK).setText("K="+K);
                
                if(!curve_plot_ArrayList.isEmpty() || !curve_ArrayList.isEmpty()) initializeArray();
                generateCurves(K);
                /*try {
                    generateCurvesWithThreads(K);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
                }*/
                revalidate();
                repaint();
                
                checkK(K);
                addResult2Table(K, MainFrame.stateOfK); 
                
                //show Kc via dialog box//
                if(numIter!=0 && ((n==1 && "below".equals(MainFrame.stateOfK)) || (n==1 && "beyond".equals(MainFrame.stateOfK)))){
                    String resultString = "Kc is approximately " + String.format("%.10f", K);
                    JOptionPane.showMessageDialog(null, resultString, "Kc estimated", JOptionPane.INFORMATION_MESSAGE);
                    timer.cancel();
                    timer.purge();
                }
                
                updateK();
                
                numIter++;
            }   
        };
        timer.scheduleAtFixedRate(task, (long)0, (long)unit_time);
    }
    
    //table management (add calculated K)
    public void addResult2Table(double K, String state){
        (MainFrame.tableDisplayK).getColumnModel().getColumn(0).setCellRenderer(new DecimalFormatRenderer() );
        Object[] row = {K, state};
        DefaultTableModel model = (DefaultTableModel)(MainFrame.tableDisplayK).getModel();
        model.addRow(row);
    }
    //force the program to show 10 decimal places
    static class DecimalFormatRenderer extends DefaultTableCellRenderer {
        DecimalFormat formatter = new DecimalFormat( "#0.00000000000" );

        @Override
        public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
            value = formatter.format((Number)value);
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column );
        }
   }
    
    //--------------------------------------------------------------------------
    // Miscellaneous
    
    //show array of curve
    public void displayCurve(ArrayList<Double[]> curve){
        if(!curve.isEmpty()){
            for(Double[] al: curve){
                 System.out.println(al[0]+","+al[1]);
            }    
        }
        
    }
    
    //boolean to check if graph is clear
    public boolean isGraphClear(){
        return curve_ArrayList.isEmpty() && curve_plot_ArrayList.isEmpty();
    }
    
    //boolean to check if table is cleared
    public boolean isTableClear(){
        return (MainFrame.tableDisplayK).getModel().getRowCount() == 0; 
    }
    
    //boolean to check if it is in bound
    public boolean isInBound(int xs, int ys){
        return (xs>xmin & xs<xmax) & (ys>ymin & ys<ymax);
    }
    
    //conversion between SCREEN <=> CARTESIAN
    public static int x2xs(double x) {
            return (int)((x - B1)/A1);
    }
    public static int y2ys(double y) {
            return (int)((y - B2)/A2);
    }
    public static double xs2x(int xs) {
            return (double)(A1*xs + B1);
    }
    public static double ys2y(int ys) {
            return (double)(A2*ys + B2);
    }
    //-------------------------------------

    
    //invariant curve (Cartesian coordinate) for different initial conditions
    public ArrayList<Double[]> invariantCurve(double U0, double I0, double K) {
            ArrayList<Double[]> arr = new ArrayList<>();
            Double[] ci = {U0,I0}; 
            arr.add(ci);
            for(int i=1; i<this.N; i++) {
                Double[] std = standardMap(U0, I0, K);
                arr.add(std);	
                U0 = std[0]; I0 = std[1];
            }
            return arr;
    }

    //convert curve from Cartesian coor. to screen coor.
    public ArrayList<Integer[]>  invariantCurve_screen(ArrayList<Double[]> invariantCurve) {
        ArrayList<Integer[]> arr = new ArrayList<>();
        for(Double[] coordinate: invariantCurve) {
            Integer[] point = {x2xs(coordinate[0]),y2ys(coordinate[1])};
            arr.add(point);
        }
        return arr;
    }

    // find modulus of two doubles //
    public static double findMod(double a, double b){
        return a - b*Math.floor(a/b);
    }

    // standard map //
    public Double[] standardMap(double U, double I, double K) {
        double I_ = findMod(I + K*Math.sin(U) ,2*Math.PI);//I + K*Math.sin(U))% (2*Math.PI);//
        double U_ = findMod(U + I + K*Math.sin(U) ,2*Math.PI);//(U + I - K*Math.sin(U))% (2*Math.PI);////
        Double[] res = {U_,I_}; 
        return res;
    }

}

//------------------------------------  end of source code ------------------------------------- 














//------------------------------------ additional comments ------------------------------------

//draw graph here
        /*if(j==N) j=0;
        for(int i=0; i<curve_plot_ArrayList.size(); i++){
            if(j<N){
                int Us = curve_plot_ArrayList.get(i).get(j)[0];
                int Is = curve_plot_ArrayList.get(i).get(j)[1];
                if(isInBound(Us,Is)) g2D.drawOval(Us, Is, MainFrame.GRID_SIZE, MainFrame.GRID_SIZE);
                if(i==curve_plot_ArrayList.size()) j++;
            }
        }*/
       
        
        /*for (ArrayList<Integer[]> al : curve_plot_ArrayList) {
            if(!al.isEmpty()){
                Iterator<Integer[]> here_point = al.iterator();
                int step=0;
                while(here_point.hasNext()){
                    Integer[] al1 = here_point.next();
                    Integer U = al1[0]; Integer I = al1[1];
                    if(step==0) g2D.setColor(Color.red); else g2D.setColor(Color.green);
                    if(isInBound(U,I)) g2D.fillOval(U,I, MainFrame.GRID_SIZE, MainFrame.GRID_SIZE);
                    step++;
                }
            }
        }*/
        
        
        /*if(curve_ArrayList.size()==3 && curve_plot_ArrayList.size()==3){
            for(int idx=0; idx<curve_plot_ArrayList.size(); idx++){
                ArrayList<Integer[]> curve = curve_plot_ArrayList.get(idx);
            }
            
                Iterator<Integer[]> here_point = curve_plot_ArrayList.get(idx).iterator();
                while(here_point.hasNext()){
                    Integer U = here_point.next()[0]; Integer I = here_point.next()[1];
                    g2D.fillOval(U,I, MainFrame.GRID_SIZE, MainFrame.GRID_SIZE);
                }   
        }*/
        
        
        /*for(int i=0; i<curve_plot_ArrayList.size(); i++){
            ArrayList<Integer[]> cp = curve_plot_ArrayList.get(i);
            for(int j=0; j<cp.size(); j++){
                if(j==0) {g2D.setColor(Color.red);} else {g2D.setColor(Color.green);}
                int U = cp.get(j)[0]; int I = cp.get(j)[1];
                if(isInBound(U, I)) g2D.fillOval(U,I, MainFrame.GRID_SIZE, MainFrame.GRID_SIZE);
            }
        }*/
        
            //int U0s = x2xs(U0); int I0s = y2ys(I0);
            
            /*if(counter>=N && !MainFrame.btnCalculate.getModel().isEnabled() 
                    && curve_ArrayList.size() == 3 && curve_plot_ArrayList.size() == 3) counter = 0;
            System.out.println(counter);
            if(!curve_plot_ArrayList.isEmpty() && counter < curve_plot_ArrayList.size()) {
                g2D.fillOval(curve_plot_ArrayList.get(1).get(counter)[0],curve_plot_ArrayList.get(1).get(counter)[1]
                        ,MainFrame.GRID_SIZE, MainFrame.GRID_SIZE); 
                counter++;
            }
            System.out.println("PLOT");*/
            
            /*if(counter>=N || MainFrame.btnCalculate.getModel().isPressed()) counter = 0;
            System.out.println( MainFrame.btnCalculate.getModel().isPressed());
            for(int index=0; index<curve_plot_ArrayList.size(); index++){
               g2D.fillOval(curve_plot_ArrayList.get(index).get(counter)[0],curve_plot_ArrayList.get(index).get(counter)[1],
                                        MainFrame.GRID_SIZE, MainFrame.GRID_SIZE); 
            }
            if(counter<N) counter++;*/
            
            
            /*
            counter = 0;
            timerGraph = new java.util.Timer();
            task = new TimerTask(){
                @Override
                public void run() {
                    /*for(int index=0; index<curve_plot_ArrayList.size(); index++) {
                        if(true){//MainFrame.GraphPanel.isIBound(U0s, I0s)
                            if(counter==0) {
                                MainFrame.isClear = false;
                                g2D.setColor(Color.red);
                            }else{
                                g2D.setColor(Color.green);
                            }

                            if (counter < N) {
                                g2D.fillOval(curve_plot_ArrayList.get(index).get(counter)[0],curve_plot_ArrayList.get(index).get(counter)[1],
                                        MainFrame.GRID_SIZE, MainFrame.GRID_SIZE);
                                GraphPanel.revalidate(); GraphPanel.repaint();
                                counter++;
                                System.out.println("PLOT");
                            }else{
                                timerGraph.cancel();
                                System.out.println("END OF PLOT");
                            }
                        }
                    }*/
                    /*if(curve_plot_ArrayList.size() ==3){
                        for(int index=0; index<3; index++) {
                            if(counter<N){
                                g2D.fillOval(curve_plot_ArrayList.get(index).get(counter)[0],
                                curve_plot_ArrayList.get(index).get(counter)[1],
                                MainFrame.GRID_SIZE, MainFrame.GRID_SIZE);
                                counter++;
                                repaint(); 
                            }
                        }  
                    }
                    if(curve_plot_ArrayList.size() ==3){
                        if(counter<N){
                            g2D.fillOval(curve_plot_ArrayList.get(1).get(counter)[0],
                            curve_plot_ArrayList.get(1).get(counter)[1],
                            MainFrame.GRID_SIZE, MainFrame.GRID_SIZE);
                            System.out.println("PLOT");
                            counter++;  
                        }
                    }
                }   
            };*/
            //timerGraph.scheduleAtFixedRate(task, (long)0, (long)(unit_time));\
            
            /*for(int index=0; index<curve_plot_ArrayList.size(); index++){
                if(curve_plot_ArrayList.size() ==3){
                    if(counter<N){
                        int U = curve_plot_ArrayList.get(index).get(counter)[0];
                        int I = curve_plot_ArrayList.get(index).get(counter)[1];
                        g2D.fillOval(U,I,MainFrame.GRID_SIZE, MainFrame.GRID_SIZE);
                        System.out.println(U+","+I);
                        System.out.println(counter);
                    }
                    if(counter<N) counter++; else counter=0;
                }
            }*/
            
        //----------------------------------------------------------------------

/*counter = 0;
            timerGraph = new java.util.Timer("Timer4Graph");
            task = new TimerTask() {
                @Override
                public void run() {
                    //System.out.println(".run()");
                    for(int index=0; index<curve_plot_ArrayList.size(); index++){
                        if(MainFrame.btnStop.getModel().isPressed()) counter = N;
                        
                        Integer[] c = curve_plot.get(counter);
                        if(counter<N && isInBound(c[0], c[1])){
                            curve_plot_ArrayList.get(index).add(c);
                            repaint(); counter++;
                        }else{
                            System.out.println("end of plot");
                            timerGraph.cancel();
                            timerGraph.purge();
                            return;
                        }
                    }
                    System.out.println("finished");
                }
            };
            
            timerGraph.scheduleAtFixedRate(task, (long)0, (long)unit_time);
            */
            
            /*new java.util.Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for(int index=0; index<curve_plot_ArrayList.size(); index++){
                            if(MainFrame.btnStop.getModel().isPressed()) counter = N;

                            if(counter<N && isInBound(curve_plot.get(counter)[0], curve_plot.get(counter)[1])){
                                curve_plot_ArrayList.get(index).add(curve_plot.get(counter));
                                counter++;System.out.println(curve_plot_ArrayList.get(index));
                            }else{
                                counter=0;
                            }
                        }
                    }
                });
            }
            }, (long)0, (long)unit_time);*/

/*
    private void displayCoordinate(double x, double y){
        MainFrame.LabelCoordinate.setText("(x,y) = (" + new DecimalFormat("#.#####").format(x) + "," + new DecimalFormat("#.###").format(y) + ")");
    }*/

// Timer for plot each curves
        /*ActionListener delayedPaint = (ActionEvent e) -> {
            if(currentIndex < this.N) currentIndex++;
            repaint();
        };
        timer = new Timer(unit_time, delayedPaint);
        timer.setInitialDelay(1000);
        timer.setDelay(unit_time);
        
        timer.start();

//function to generate graph from all possible initial conditions
    private void generateCurvesParallel(double K) { 
        Thread threads[] = new Thread[numCurve];
        System.out.println("Numcurve: "+numCurve);
        for(int i=0; i<numCurve; i++) {
            final int index = i;
            Runnable r = () -> { generateGraph(3.14, index*h, K);};
            threads[i] = new Thread(r);
            threads[i].start();
        }
        
        for(int i=0;i<numCurve ; i++){
            try {
                threads[i].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    
    //sort the "sorted" array of point from right to left. bottom to top
    private ArrayList<Double[]> sortBackwardCurve(ArrayList<Double[]> sortCurve){
        ArrayList<Double[]> newArr = new ArrayList<>(sortCurve);
        for(int i=0; i<newArr.size()/2; i++) {
            Collections.swap(newArr, i, newArr.size()-1-i);
        }
        return newArr;
    }
//------------------------------------ additional comments ------------------------------------
*/