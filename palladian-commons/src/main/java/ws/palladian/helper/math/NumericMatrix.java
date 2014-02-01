package ws.palladian.helper.math;

import java.io.Serializable;

import ws.palladian.helper.collection.MapMatrix;
import ws.palladian.helper.collection.Matrix;
import ws.palladian.helper.collection.MatrixDecorator;

public class NumericMatrix<K> extends MatrixDecorator<K, Double> implements Serializable {

    private static final long serialVersionUID = 1L;

    public NumericMatrix() {
        this(new MapMatrix<K, Double>());
    }

    public NumericMatrix(Matrix<K, Double> matrix) {
        super(matrix);
    }

    /**
     * <p>
     * Add each cell of the given matrix to the current one.
     * </p>
     * 
     * @param matrix The matrix to add to the current matrix. The matrix must have the same column and row names as the
     *            matrix it is added to.
     */
    public void add(NumericMatrix<K> matrix) {

        for (K yKey : getRowKeys()) {
            for (K xKey : getColumnKeys()) {
                Double currentNumber = get(xKey, yKey);
                double value = currentNumber.doubleValue();
                Double number = matrix.get(xKey, yKey);

                // in that case one matrix did not have that cell and we create it starting from zero
                if (number == null) {
                    number = 0.;
                }

                value += number.doubleValue();
                set(xKey, yKey, value);
            }
        }

    }

    /**
     * <p>
     * Divide each cell of the given matrix by the given number.
     * </p>
     * 
     * @param divisor The value by which every cell is divided by.
     */
    public void divideBy(double divisor) {
        for (K yKey : getRowKeys()) {
            for (K xKey : getColumnKeys()) {
                Double currentNumber = get(xKey, yKey);
                double value = currentNumber.doubleValue();
                value /= divisor;
                set(xKey, yKey, value);
            }
        }
    }

    @Override
    public Double get(K x, K y) {
        Double value = matrix.get(x, y);
        return value != null ? value : 0;
    };

    public static void main(String[] args) {

        NumericMatrix<String> confusionMatrix = new NumericMatrix<String>();

        Double o = confusionMatrix.get("A", "B");
        if (o == null) {
            o = 1.;
        } else {
            o = o + 1;
        }
        confusionMatrix.set("A", "B", o);

        o = confusionMatrix.get("B", "A");
        if (o == null) {
            o = 1.;
        } else {
            o = o + 1;
        }
        confusionMatrix.set("B", "A", o);

        o = confusionMatrix.get("B", "B");
        if (o == null) {
            o = 1.;
        } else {
            o = o + 1;
        }
        confusionMatrix.set("B", "B", o);

        o = confusionMatrix.get("B", "B");
        if (o == null) {
            o = 1.;
        } else {
            o = o + 1;
        }
        confusionMatrix.set("B", "B", o);

        System.out.println(confusionMatrix);

        Matrix<String, String> confusionMatrix2 = new MapMatrix<String, String>();
        confusionMatrix2.set("A", "1", "A1");
        confusionMatrix2.set("B", "2", "B2");
        System.out.println(confusionMatrix2);

    }
}
