package ravensproject;

// Uncomment these lines to access image processing.
//import java.awt.Image;
//import java.io.File;
//import javax.imageio.ImageIO;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Your Agent for solving Raven's Progressive Matrices. You MUST modify this
 * file.
 *
 * You may also create and submit new files in addition to modifying this file.
 *
 * Make sure your file retains methods with the signatures:
 * public Agent()
 * public char Solve(RavensProblem problem)
 *
 * These methods will be necessary for the project's main method to run.
 *
 */
public class Agent {

    enum TransType {
        HORIZONTAL_REFLECT,
        VERTICAL_REFLECT,
        ROTATE_90,
        ROTATE_180,
        ROTATE_270,
        NONE
    }

    enum PatternType {
        UNKNOWN,
        DIAGONAL,
        INCREASING_BLACK,
        NONE,
        ADDITION,
        SUBTRACTION,
        AND,
        XOR
    }

    final int BLACK = Color.BLACK.getRGB();
    final int WHITE = Color.WHITE.getRGB();

    /**
     * The default constructor for your Agent. Make sure to execute any
     * processing necessary before your Agent starts solving problems here.
     * <p>
     * Do not add any variables to this signature; they will not be used by
     * main().
     */
    public Agent() {

    }

    /**
     * The primary method for solving incoming Raven's Progressive Matrices.
     * For each problem, your Agent's Solve() method will be called. At the
     * conclusion of Solve(), your Agent should return an int representing its
     * answer to the question: 1, 2, 3, 4, 5, or 6. Strings of these ints
     * are also the Names of the individual RavensFigures, obtained through
     * RavensFigure.getName(). Return a negative number to skip a problem.
     * <p>
     * Make sure to return your answer *as an integer* at the end of Solve().
     * Returning your answer as a string may cause your program to crash.
     *
     * @param problem the RavensProblem your agent should solve
     * @return your Agent's answer to this problem
     */
    public int Solve(RavensProblem problem) {
        System.out.println("- - -");
        System.out.println(problem.getName());


        if (problem.getProblemType().equals("3x3")) {
            return solve3x3(problem);
        }

        return -1;
    }

    private int solve3x3(RavensProblem problem) {
        HashMap<String, RavensFigure> figures = problem.getFigures();

        PatternType pattern = determine3x3Pattern(figures);
        System.out.println("pattern = " + pattern);

        int[] choices = {1, 2, 3, 4, 5, 6, 7, 8};
        int answer;
        switch (pattern) {

            case UNKNOWN: {
                answer = findAnswerChoiceClosestTo("E", choices, figures);
                break;
            }
            case NONE: {
                answer = findAnswerChoiceClosestTo("H", choices, figures);
                break;

            }
            case INCREASING_BLACK: {
                double blackPercentage = getExpectedBlackPercentage(figures, "E", "F", "H");
                answer = getAnswerByBlackPercentage(figures, choices, blackPercentage);
                break;
            }
            case DIAGONAL: {
                // found diagonal pattern
                // now look for which answer choice closely matches A-E
                // instead maybe, let's look for an answer that has the same delta from A to E.

                double blackPercentage = getExpectedBlackPercentage(figures, "A", "E", "E");
//                answer = findAnswerChoiceClosestTo("E", choices, figures);
                answer = getAnswerByBlackPercentage(figures, choices, blackPercentage);
                break;

            }

            case ADDITION: {
                BufferedImage G = getImage(figures.get("G"));
                BufferedImage H = getImage(figures.get("H"));
                BufferedImage expectedImage = addImages(G, H);

//                printImage(expectedImage, problem.getName().substring(14));

                answer = findAnswerChoiceClosestTo(expectedImage, choices, figures);
                break;
            }

            case SUBTRACTION: {
                BufferedImage G = getImage(figures.get("G"));
                BufferedImage H = getImage(figures.get("H"));
                BufferedImage expectedImage = subtractImages(G, H);

//                printImage(expectedImage, problem.getName().substring(14));

                answer = findAnswerChoiceClosestTo(expectedImage, choices, figures);
                break;
            }

            case XOR: {
                BufferedImage G = getImage(figures.get("G"));
                BufferedImage H = getImage(figures.get("H"));
                BufferedImage expectedImage = xorImages(G, H);

//                printImage(expectedImage, problem.getName().substring(14));

                answer = findAnswerChoiceClosestTo(expectedImage, choices, figures);
                break;
            }

            case AND: {
                BufferedImage G = getImage(figures.get("G"));
                BufferedImage H = getImage(figures.get("H"));
                BufferedImage expectedImage = andImages(G, H);

//                printImage(expectedImage, problem.getName().substring(14));

                answer = findAnswerChoiceClosestTo(expectedImage, choices, figures);
                break;
            }

            default: {
                answer = -1;
            }
        }

        System.out.println("answer = " + answer);
        return answer;
    }

    private double getExpectedBlackPercentage(HashMap<String, RavensFigure> figures, String x1, String x2, String y1) {
        double blackPercentageE = getBlackPixelPercentage(getImage(figures.get(x1)));
        double blackPercentageF = getBlackPixelPercentage(getImage(figures.get(x2)));
        double blackPercentageH = getBlackPixelPercentage(getImage(figures.get(y1)));

        double expectedBlackPixelPercent = blackPercentageH + (blackPercentageF - blackPercentageE);
        System.out.println("expectedBlackPixelPercent = " + expectedBlackPixelPercent);

        return expectedBlackPixelPercent;
    }

    private int getAnswerByBlackPercentage(HashMap<String, RavensFigure> figures, int[] choices, double expectedBlackPercentage) {
        HashMap<Integer, Double> blackPercentMap = new HashMap<>();
        for (int i : choices) {
            BufferedImage current = getImage(figures.get(Integer.toString(i)));

            double blackPercentageI = getBlackPixelPercentage(current);
            System.out.println("Answer choice " + i);
            System.out.println("blackPercentageI = " + blackPercentageI);
            blackPercentMap.put(i, blackPercentageI);
        }

        System.out.println("blackPercentMap = " + blackPercentMap);
        double closestDiff = 100.0;
        int key = -1;
        for (int i : choices) {
            double current = blackPercentMap.get(i);
            double diff = Math.abs(expectedBlackPercentage - current);

            if (diff < closestDiff) {
                closestDiff = diff;
                key = i;
            }
        }

        return key;
    }

    private PatternType determine3x3Pattern(HashMap<String, RavensFigure> figures) {
        BufferedImage A = getImage(figures.get("A"));
        BufferedImage B = getImage(figures.get("B"));
        BufferedImage C = getImage(figures.get("C"));
        BufferedImage D = getImage(figures.get("D"));
        BufferedImage E = getImage(figures.get("E"));
        BufferedImage F = getImage(figures.get("F"));
        BufferedImage G = getImage(figures.get("G"));
        BufferedImage H = getImage(figures.get("H"));

        TransType transType = identifyTrans(A, B);
        System.out.println("A-E transType = " + transType);

        PatternType row1 = identifySubPattern(A, B, C);
        System.out.println("row1 = " + row1);
        PatternType row2 = identifySubPattern(D, E, F);
        System.out.println("row2 = " + row2);
        

        if (PatternType.ADDITION == row1)
            return row1;
        if (PatternType.SUBTRACTION == row1)
            return row1;
        if (PatternType.XOR == row1)
            return row1;
        if (PatternType.AND == row1)
            return row1;
        else if (PatternType.NONE == row1 && PatternType.NONE == row2)
            return PatternType.NONE;
        else if (transType == TransType.NONE)
            return PatternType.DIAGONAL;
        else if (row1 != row2)
            return PatternType.DIAGONAL;
        else {
            PatternType col1 = identifySubPattern(A, D, G);
            System.out.println("col1 = " + col1);
            PatternType col2 = identifySubPattern(B, E, H);
            System.out.println("col2 = " + col2);

            if (PatternType.INCREASING_BLACK == col1 && PatternType.INCREASING_BLACK == col2)
                return PatternType.INCREASING_BLACK;
            else if (PatternType.INCREASING_BLACK == col2)
                return PatternType.DIAGONAL;
        }

        return PatternType.UNKNOWN;
    }

    private int findAnswerChoiceClosestTo(BufferedImage x, int[] choices, HashMap<String, RavensFigure> figures){
        return findAnswerChoiceClosestTo(null, x, choices, figures);
    }

    private int findAnswerChoiceClosestTo(String Xname, int[] choices, HashMap<String, RavensFigure> figures){
        return findAnswerChoiceClosestTo(Xname, null, choices, figures);
    }

    private int findAnswerChoiceClosestTo(String Xname, BufferedImage X, int[] choices, HashMap<String, RavensFigure> figures){
        HashMap<Integer, Double> similarityScoreMap = new HashMap<>();

        X = X != null ? X : getImage(figures.get(Xname));
        Xname = Xname != null ? Xname : "X";

        for (int i : choices) {
            double similarityOfCn = compareSimilarity(X, getImage(figures.get(Integer.toString(i))));
            similarityScoreMap.put(i, similarityOfCn);
            System.out.println("similarityOf" + Xname + i + " = " + similarityOfCn);
        }

        // Next check for the closest similarity score to AB
        double closestDiff = 100.0;
        int key = -1;
        for (int i : choices) {
            double current = similarityScoreMap.get(i);
            double diff = Math.abs(100.0 - current);

            if (diff < closestDiff) {
                closestDiff = diff;
                key = i;
            }
        }

        return key;
    }




    private PatternType identifySubPattern(BufferedImage A, BufferedImage B, BufferedImage C) {
        BufferedImage subtraction = subtractImages(A, B);
        double subtractionSimilarity = compareSimilarity(subtraction, C);
        System.out.println("subtractionSimilarity = " + subtractionSimilarity);

        BufferedImage addition = addImages(A, B);
        double additionSimilarity = compareSimilarity(addition, C);
        System.out.println("additionSimilarity = " + additionSimilarity);

        BufferedImage xor = xorImages(A, B);
        double xorSimilarity = compareSimilarity(xor, C);
        System.out.println("xorSimilarity = " + xorSimilarity);

        BufferedImage and = andImages(A, B);
        double andSimilarity = compareSimilarity(and, C);
        System.out.println("andSimilarity = " + andSimilarity);


        if (xorSimilarity > additionSimilarity)
            return PatternType.XOR;

        if (andSimilarity > additionSimilarity)
            return PatternType.AND;

        if (additionSimilarity > 90 && additionSimilarity > subtractionSimilarity)
            return PatternType.ADDITION;
        else if (subtractionSimilarity > 90 && subtractionSimilarity > additionSimilarity)
            return PatternType.SUBTRACTION;




        double blackLevelA = getBlackPixelPercentage(A);
        double blackLevelB = getBlackPixelPercentage(B);
        double blackLevelC = getBlackPixelPercentage(C);

        System.out.println("blackLevelA = " + blackLevelA + " blackLevelB = " + blackLevelB  + " blackLevelC = " + blackLevelC);

        
        double blackDiffBA = blackLevelB - blackLevelA;
        System.out.println("blackDiffBA = " + blackDiffBA);
        double blackDiffCB = blackLevelC - blackLevelB;
        System.out.println("blackDiffCB = " + blackDiffCB);

        double simAB = compareSimilarity(A, B);
        double simBC = compareSimilarity(B, C);
        double simAC = compareSimilarity(A, C);
        System.out.println("simAB = " + simAB + " simBC = " + simBC + " simAC = " + simAC);

        PatternType ret;

        if (blackLevelB > blackLevelA && blackLevelC > blackLevelB){
//            System.out.println("This row has pattern INCREASING");
            ret = PatternType.INCREASING_BLACK;
        } else if ((blackDiffBA == 0.0 && blackDiffCB == 0.0)) {
//            System.out.println("This row has pattern NONE");
            ret = PatternType.NONE;
        } else {
            ret = PatternType.UNKNOWN;
        }

        return ret;
    }

    private double compareSimilarity(BufferedImage image1, BufferedImage image2) {
        long rawdiff = 0;

        for (int x = 0; x < image1.getWidth(); x++) {
            for (int y = 0; y < image1.getHeight(); y++) {
                int pixel1 = image1.getRGB(x, y);
                int pixel2 = image2.getRGB(x, y);

                rawdiff += pixelDiff(pixel1, pixel2);

            }
        }

        long maxDiff = 3L * 255 * image1.getWidth() * image1.getHeight();
        double diffPercentage = 100.0 * rawdiff / maxDiff;
        double similarityPercentage = 100.0 - diffPercentage;

        return similarityPercentage;
    }

    private int pixelDiff(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >>  8) & 0xff;
        int b1 =  rgb1        & 0xff;
        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >>  8) & 0xff;
        int b2 =  rgb2        & 0xff;
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }


    private BufferedImage getImage(RavensFigure fig) {
        try {
            BufferedImage image = ImageIO.read(new File(fig.getVisual()));
            return image;

        } catch(Exception ex) {}

        return null;
    }


    private TransType identifyTrans(BufferedImage first, BufferedImage second) {

        // These should be in order of importance
        // LinkedHashMap preserves the order
        LinkedHashMap<TransType, Double> transScoresMap = new LinkedHashMap<>();
        transScoresMap.put(TransType.NONE, compareSimilarity(first, second));

        BufferedImage flipHoriz = applyTransType(TransType.HORIZONTAL_REFLECT, first);
        transScoresMap.put(TransType.HORIZONTAL_REFLECT, compareSimilarity(flipHoriz, second));

        BufferedImage vertReflect = applyTransType(TransType.VERTICAL_REFLECT, first);
        transScoresMap.put(TransType.VERTICAL_REFLECT, compareSimilarity(vertReflect, second));

        BufferedImage rotated90 = applyTransType(TransType.ROTATE_90, first);
        transScoresMap.put(TransType.ROTATE_90, compareSimilarity(rotated90, second));

        BufferedImage rotated180 = applyTransType(TransType.ROTATE_180, first);
        transScoresMap.put(TransType.ROTATE_180, compareSimilarity(rotated180, second));

        BufferedImage rotated270 = applyTransType(TransType.ROTATE_270, first);
        transScoresMap.put(TransType.ROTATE_270, compareSimilarity(rotated270, second));

//        System.out.println("transScoresMap = " + transScoresMap);

        Map.Entry<TransType, Double> maxEntry = null;
        for (Map.Entry<TransType, Double> entry : transScoresMap.entrySet())
        {
            // if multiple have the same value, this will return the first
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
            {
                maxEntry = entry;
            }
        }

        return maxEntry.getKey();
    }

    private BufferedImage applyTransType(TransType type, BufferedImage image) {
        final BufferedImage transformed;
        switch(type) {
            case HORIZONTAL_REFLECT: {
                transformed = applyHorizontalReflect(image);
                break;
            }

            case VERTICAL_REFLECT: {
                transformed = applyVerticalReflect(image);
                break;
            }

            case ROTATE_90: {
                transformed = apply90Rotation(image);
                break;
            }

            case ROTATE_180: {
                transformed = apply180Rotation(image);
                break;
            }

            case ROTATE_270: {
                transformed = apply270Rotation(image);
                break;
            }

            default: {
                transformed = image;
                break;
            }
        }

        return transformed;
    }

    private BufferedImage apply90Rotation(BufferedImage image) {
        return applyRotation(image, Math.PI/2);
    }

    private BufferedImage apply270Rotation(BufferedImage image) {
        return applyRotation(image, 3*Math.PI/2);
    }

    private BufferedImage apply180Rotation(BufferedImage image) {
        return applyRotation(image, Math.PI);
    }

    private BufferedImage applyRotation(BufferedImage image, double radians) {
        AffineTransform affineTransform = new AffineTransform();
        affineTransform.translate(image.getHeight() / 2, image.getWidth() / 2);
        affineTransform.rotate(radians);
        affineTransform.translate(-image.getWidth() / 2, -image.getHeight() / 2);

        AffineTransformOp affineTransformOp = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);

        BufferedImage result = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());
        affineTransformOp.filter(image, result);

        return result;
    }


    private BufferedImage applyHorizontalReflect(BufferedImage image) {
        BufferedImage clone = cloneImage(image);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int pixel = image.getRGB(image.getWidth() - x - 1, y);
                clone.setRGB(x, y, pixel);
            }
        }

        return clone;
    }

    private BufferedImage applyVerticalReflect(BufferedImage image) {
        BufferedImage clone = cloneImage(image);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int pixel = image.getRGB(x, image.getWidth() - y - 1);
                clone.setRGB(x, y, pixel);
            }
        }

        return clone;
    }

    private BufferedImage addImages(BufferedImage first, BufferedImage second) {
        BufferedImage clone = cloneImage(first);

        for (int x = 0; x < clone.getWidth(); x++) {
            for (int y = 0; y < clone.getHeight(); y++) {
                int pixel1 = first.getRGB(x, y);
                int pixel2 = second.getRGB(x, y);

                if (BLACK == pixel1 || BLACK  == pixel2)
                    clone.setRGB(x, y, Color.BLACK.getRGB());
            }
        }

        return clone;
    }

    private BufferedImage subtractImages(BufferedImage first, BufferedImage second) {
        BufferedImage clone = cloneImage(first);

        for (int x = 0; x < clone.getWidth(); x++) {
            for (int y = 0; y < clone.getHeight(); y++) {
                int pixel1 = first.getRGB(x, y);
                int pixel2 = second.getRGB(x, y);

                if (BLACK == pixel1 && BLACK  == pixel2)
                    clone.setRGB(x, y, WHITE);
            }
        }

        return clone;
    }

    private BufferedImage xorImages(BufferedImage first, BufferedImage second) {
        BufferedImage clone = cloneImage(first);

        for (int x = 0; x < clone.getWidth(); x++) {
            for (int y = 0; y < clone.getHeight(); y++) {
                int pixel1 = first.getRGB(x, y);
                int pixel2 = second.getRGB(x, y);

                if (BLACK == pixel1 || BLACK  == pixel2)
                    clone.setRGB(x, y, BLACK);
                if (BLACK == pixel1 && BLACK  == pixel2)
                    clone.setRGB(x, y, WHITE);
            }
        }

        return clone;
    }

    private BufferedImage andImages(BufferedImage first, BufferedImage second) {
        BufferedImage clone = cloneImage(first);

        for (int x = 0; x < clone.getWidth(); x++) {
            for (int y = 0; y < clone.getHeight(); y++) {
                int pixel1 = first.getRGB(x, y);
                int pixel2 = second.getRGB(x, y);

                if (BLACK == pixel1 && BLACK  == pixel2)
                    clone.setRGB(x, y, BLACK);
                else clone.setRGB(x, y, WHITE);
            }
        }

        return clone;
    }

    private BufferedImage cloneImage(BufferedImage image) {
        ColorModel model = image.getColorModel();
        WritableRaster raster = image.copyData(null);
        BufferedImage clone = new BufferedImage(model, raster, model.isAlphaPremultiplied(), null);
        return clone;
    }

    private double getBlackPixelPercentage(BufferedImage image) {
        long blackPixels = 0;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int pixel = image.getRGB(x, y);
                if (BLACK == pixel) blackPixels++;
            }
        }

        long totalPixels = image.getWidth() * image.getHeight();
        return 100.0 * blackPixels / totalPixels;
    }

    private void printImage(BufferedImage image, String fileName) {
        try {
            File outputfile = new File(fileName + ".png");
            ImageIO.write(image, "png", outputfile);
            System.out.println("printed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

