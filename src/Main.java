
import java.awt.Canvas;
import java.awt.Color;
import java.io.IOException;
import java.util.Scanner;
import javax.swing.JFrame;

public class Main {

    public static void main(String[] args) {
        if (args.length != 2)
        {
            System.out.println("Please specify a path to the data set and the desired number of clusters.\n");
            System.exit(-1);
        }
        
        int numClusters = 0;
        try
        {
            numClusters = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("Desired number of clusters must be a valid integer value.");
            System.exit(-1);
        }
        
        String filename = args[0];
        
        DataSet dataSet = null;
        
        try
        {
            dataSet = DataSet.importFromFile(filename);
        }
        catch (IOException ioe)
        {
            System.out.println("Error reading file: " + ioe.getMessage());
            System.exit(-1);
        }
        
        Chromosome.ReproductionAgent<ClusterChromosome> repAgent = null;
        Chromosome.MutationAgent<ClusterChromosome> mutAgent = null;
        ChromosomeSelector<ClusterChromosome> selector = null;
        CentroidGenerator centGen = null;
        
        Scanner in = new Scanner(System.in);
        
        boolean ready = false;
        
        System.out.println("Choose a Centroid Generation Algorithm:");
        System.out.println("\t1. Random centroids from within data set bounds.");
        System.out.println("\t2. Pick random points from data set at centroids.");
        while (!ready)
        {
            System.out.print("> ");

            ready = true;
            
            switch (in.nextInt())
            {
                case 1:
                    centGen = new CentroidGenerator.Random(dataSet);
                    break;
                case 2:
                    centGen = new CentroidGenerator.Shuffle(dataSet);
                    break;
                default:
                    System.out.println("Invalid Input.");
                    ready = false;
                    break;
            }
        }
        
        ready = false;
        System.out.println("Choose a Parent Selector:");
        System.out.println("\t1. Roulette.");
        System.out.println("\t2. Tournament.");
        while (!ready)
        {
            System.out.print("> ");

            ready = true;
            
            switch (in.nextInt())
            {
                case 1:
                    selector = new ChromosomeSelector.Roulette<ClusterChromosome>();
                    break;
                case 2:
                    System.out.print("What will be the tournament size? ");
                    selector = new ChromosomeSelector.Tournament<ClusterChromosome>(in.nextInt());
                    break;
                default:
                    System.out.println("Invalid Input.");
                    ready = false;
                    break;
            }
        }
        
        System.out.print("Population Size? ");
        int popSize = in.nextInt();
        
        System.out.print("How many Iterations? ");
        int maxIterations = in.nextInt();
        
        ready = false;
        System.out.println("Choose a Reproduction Agent:");
        System.out.println("\t1. One-Point Crossover.");
        System.out.println("\t2. Average of parents.");
        while (!ready)
        {
            System.out.print("> ");

            ready = true;
            
            switch (in.nextInt())
            {
                case 1:
                    repAgent = new ClusterChromosome.OnePointCrossover();
                    break;
                case 2:
                    repAgent = new ClusterChromosome.Average();
                    break;
                default:
                    System.out.println("Invalid Input.");
                    ready = false;
                    break;
            }
        }
        
        ready = false;
        System.out.println("Choose a Mutation Agent:");
        System.out.println("\t1. Swap.");
        System.out.println("\t2. Gaussian Noise.");
        while (!ready)
        {
            System.out.print("> ");

            ready = true;
            
            switch (in.nextInt())
            {
                case 1:
                    mutAgent = new ClusterChromosome.Swap();
                    break;
                case 2:
                    mutAgent = new ClusterChromosome.GaussianNoise();
                    break;
                default:
                    System.out.println("Invalid Input.");
                    ready = false;
                    break;
            }
        }
        
        
        GeneticAlgorithm<ClusterChromosome> ga = new GeneticAlgorithm<ClusterChromosome>(
                new ClusterChromosome.CentroidPopulationGenerator(
                    dataSet,
                    numClusters,
                    centGen,
                    popSize),
                repAgent,
                mutAgent,
                selector);
        
        // statistics...
        float[] interConnections = new float[maxIterations+1];
        float[] intraConnections = new float[maxIterations+1];
        float[] quantizationErrors = new float[maxIterations+1];
        
        float mutationProbability = 1.0f;
        
        float percentDone = 0.0f;
        float percent = 0.0f;
        
        // commence!
        for (int i = 0; i < maxIterations; i++)
        {
            ClusterChromosome best = ga.getBest();
            
            // collect stats
            interConnections[i] = best.getInterClusterDistance();
            intraConnections[i] = best.getIntraClusterDistance();
            quantizationErrors[i] = best.getQuatizationError();
            
            // perform GA
            ga.nextGeneration(mutationProbability);
            
            mutationProbability -= 0.99f/maxIterations;
            
            percentDone += 100.0f/(float)maxIterations;
            percent += 100.0f/(float)maxIterations;
            if (percent > 5.0f)
            {
                percent -= 5.0f;
                System.out.print(Math.round(percentDone) + "% ... ");
            }
        }
        System.out.println("DONE!");
        
        ClusterChromosome best = ga.getBest();

        // collect stats
        interConnections[maxIterations] = best.getInterClusterDistance();
        intraConnections[maxIterations] = best.getIntraClusterDistance();
        quantizationErrors[maxIterations] = best.getQuatizationError();
        
        System.out.println("Inter-Cluster Distance: " + interConnections[maxIterations]);
        System.out.println("Intra-Cluster Distance: " + intraConnections[maxIterations]);
        System.out.println("Quantization Errors: " + quantizationErrors[maxIterations]);
        
        Canvas canvas = dataSet.getVisualRepresentation(best.getCentroids());
        
        if (canvas != null)
        {
            JFrame view = new JFrame("DataSet: " + filename);

            view.add(canvas);
            
            view.setSize(800,600);
            
            view.setVisible(true);
            
            view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
        else   
        {
            System.out.println("Centroids:");
            float[][] centroids = best.getCentroids();
            for (int i = 0; i < centroids.length; i++)
            {
                for (int j = 0; j < centroids[i].length; j++)
                {
                    System.out.print(centroids[i][j]+"\t");
                }
                System.out.println();
            }
        }
        
        Canvas[] canvases = new Canvas[]{
            new ArrayGraph(Color.blue, interConnections),
            new ArrayGraph(Color.red, intraConnections),
            new ArrayGraph(Color.orange, quantizationErrors)
        };
        
        for (Canvas canv :canvases)
        {
            JFrame view = new JFrame("Some Canvas: " + filename);

            view.add(canv);
            
            view.setSize(800,600);
            
            view.setVisible(true);
        }
        
        
    }
}