package cmm.compiler.utillity;

/**
 * Used to pair two different objects together.
 * @param <T> Type of the left object.
 * @param <U> Type of the right object.
 * 
 * @author Leslie Marxen
 */
public class Pair<T, U>{
    private final T left;
    private final U right;

    /**
     * Constructor that initializes left and right.
     * @param left An object that fills the left value,
     * @param right An object that fills the right value.
     */
    public Pair(T left, U right){
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the left value.
     * @return the left value.
     */
    public T getLeft(){
        return left;
    }

    /**
     * Returns the right value.
     * @return the right value.
     */
    public U getRight(){
        return right;
    }

    
    /**
     * Calls the equals method of left and right comparing if both pairs store equal data.
     * @return true if both pairs are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && getClass() == obj.getClass()) {
            Pair<?, ?> o = (Pair<?, ?>)obj;
            boolean b;
            b  = o.left.equals(left);
            b &= o.right.equals(right);
            
            return b;
        }
        return false;
    }

    /**
     * Returns the a String representation of this object.<br>
     * Format: {@code <left.toString()|right.toString()>}
     */
    @Override
    public String toString() {
        return new StringBuilder().append("<")
            .append(left.toString()).append("|")
            .append(right.toString()).append(">")
            .toString();
    }
}