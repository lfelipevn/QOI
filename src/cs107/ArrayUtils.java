package cs107;

/**
 * Utility class to manipulate arrays.
 * @apiNote First Task of the 2022 Mini Project
 * @author Hamza REMMAL (hamza.remmal@epfl.ch)
 * @version 1.0
 * @since 1.0
 */
public final class ArrayUtils {

    /**
     * DO NOT CHANGE THIS, MORE ON THAT IN WEEK 7.
     */
    private ArrayUtils(){}

    // ==================================================================================
    // =========================== ARRAY EQUALITY METHODS ===============================
    // ==================================================================================

    /**
     * Check if the content of both arrays is the same
     * @param a1 (byte[]) - First array
     * @param a2 (byte[]) - Second array
     * @return (boolean) - true if both arrays have the same content (or both null), false otherwise
     * @throws AssertionError if one of the parameters is null
     */
    public static boolean equals(byte[] a1, byte[] a2){
        assert !((a1 == null && a2 != null) || (a1 != null && a2 == null));
        if (a1 == null){
            return true;
        }
        if (a1.length != a2.length){
            return false;
        }
        for (int i = 0; i < a1.length; ++i){
            if (a1[i] != a2[i]){
                return false;
            }
        }
        return true;

    }

    /**
     * Check if the content of both arrays is the same
     * @param a1 (byte[][]) - First array
     * @param a2 (byte[][]) - Second array
     * @return (boolean) - true if both arrays have the same content (or both null), false otherwise
     * @throws AssertionError if one of the parameters is null
     */
    public static boolean equals(byte[][] a1, byte[][] a2){
        assert !((a1 == null && a2 != null) || (a1 != null && a2 == null));
        if (a1 == null){
            return true;
        }
        if (a1.length != a2.length){
            return false;
        }
        for (int i = 0; i < a1.length; ++i){
            if (a1[i].length != a2[i].length){
                return false;
            }
            for (int j = 0; j < a1[i].length; ++j){
                if (a1[i][j] != a2[i][j]){
                    return false;
                }
            }
        }
        return true;
    }

    // ==================================================================================
    // ============================ ARRAY WRAPPING METHODS ==============================
    // ==================================================================================

    /**
     * Wrap the given value in an array
     * @param value (byte) - value to wrap
     * @return (byte[]) - array with one element (value)
     */
    public static byte[] wrap(byte value){
        byte[] wrapped = {value};
        return wrapped;
    }

    // ==================================================================================
    // ========================== INTEGER MANIPULATION METHODS ==========================
    // ==================================================================================

    /**
     * Create an Integer using the given array. The input needs to be considered
     * as "Big Endian"
     * (See handout for the definition of "Big Endian")
     * @param bytes (byte[]) - Array of 4 bytes
     * @return (int) - Integer representation of the array
     * @throws AssertionError if the input is null or the input's length is different from 4
     */

    public static int toInt(byte[] bytes) {
        assert !(bytes == null || bytes.length != 4);
        int number = (bytes[0]  & 0xFF) << 8*3;
        for (int i = 1; i <= 3; ++i){
            number = number | ((bytes[i] & 0xFF) << (8*(3-i))) ;
        }
        return number;
    }
    /**
     * Separate the Integer (word) to 4 bytes. The Memory layout of this integer is "Big Endian"
     * (See handout for the definition of "Big Endian")
     * @param value (int) - The integer
     * @return (byte[]) - Big Endian representation of the integer
     */
    public static byte[] fromInt(int value){
        byte[] bytes = new byte[4];
        for (int i = 0; i < bytes.length; ++i){
            bytes[i] = (byte) (0b1111_1111 & value >> 8*(3-i));
        }
        return bytes;
    }

    // ==================================================================================
    // ========================== ARRAY CONCATENATION METHODS ===========================
    // ==================================================================================

    /**
     * Concatenate a given sequence of bytes and stores them in an array
     * @param tabs (byte ...) - Sequence of bytes to store in the array
     * @return (byte[]) - Array representation of the sequence
     * @throws AssertionError if the input is null
     */
    public static byte[] concat(byte ... tabs){
        assert !(tabs == null);
        byte[] concatenated = new byte[tabs.length];
        for (int i = 0; i < concatenated.length; i++){
            concatenated[i] = tabs[i];
        }
        return concatenated;
    }

    /**
     * Concatenate a given sequence of arrays into one array
     * @param bytes (byte[] ...) - Sequence of arrays
     * @return (byte[]) - Array representation of the sequence
     * @throws AssertionError if the input is null
     * or one of the inner arrays of input is null.
     */
    public static byte[] concat(byte[] ... bytes){
        assert !(bytes == null);
        int n = 0;
        for (int i = 0; i < bytes.length; ++i){
            assert !(bytes[i] == null);
            n += bytes[i].length;
        }
        byte[] concatenation = new byte[n];
        int cont = 0;
        for (int i = 0; i< bytes.length; ++i){
            for (int j = 0; j< bytes[i].length; ++j){
                concatenation[cont+j] = bytes[i][j];
            }
            cont += bytes[i].length;
        }
        return concatenation;
    }

    // ==================================================================================
    // =========================== ARRAY EXTRACTION METHODS =============================
    // ==================================================================================

    /**
     * Extract an array from another array
     * @param input (byte[]) - Array to extract from
     * @param start (int) - Index in the input array to start the extract from
     * @param length (int) - The number of bytes to extract
     * @return (byte[]) - The extracted array
     * @throws AssertionError if the input is null or start and length are invalid.
     * start + length should also be smaller than the input's length
     */
    public static byte[] extract(byte[] input, int start, int length){
        assert !(input == null || start + length > input.length || start < 0 || start > input.length || length <=0);
        byte[] extraction = new byte[length];
        for (int i = 0; i < extraction.length; ++i){
            extraction[i] = input[start + i];
        }
        return extraction;
    }

    /**
     * Create a partition of the input array.
     * (See handout for more information on how this method works)
     * @param input (byte[]) - The original array
     * @param sizes (int ...) - Sizes of the partitions
     * @return (byte[][]) - Array of input's partitions.
     * The order of the partition is the same as the order in sizes
     * @throws AssertionError if one of the parameters is null
     * or the sum of the elements in sizes is different from the input's length
     */
    public static byte[][] partition(byte[] input, int ... sizes) {
        assert !(input == null || sizes == null);
        int sumSizes = 0;
        for (int i = 0; i < sizes.length; ++i){
            sumSizes += sizes[i];
        }
        assert (sumSizes == input.length);

        byte[][] partitionArray = new byte[sizes.length][];
        int cont = 0;
        for (int i = 0; i < sizes.length; ++i){
            partitionArray[i] = extract(input, cont, sizes[i]);
            cont += sizes[i];
        }

        return partitionArray;
    }

    // ==================================================================================
    // ============================== ARRAY FORMATTING METHODS ==========================
    // ==================================================================================

    /**
     * Format a 2-dim integer array
     * where each dimension is a direction in the image to
     * a 2-dim byte array where the first dimension is the pixel
     * and the second dimension is the channel.
     * See handouts for more information on the format.
     * @param input (int[][]) - image data
     * @return (byte [][]) - formatted image data
     * @throws AssertionError if the input is null
     * or one of the inner arrays of input is null
     */
    public static byte[][] imageToChannels(int[][] input){
        assert !(input == null);
        int width = input[0].length;
        for (int i = 0; i < input.length; ++i) {
            assert !(input[i] == null || input[i].length != width);
        }
        byte[][] channels = new byte[input.length * input[0].length][4];

        for (int j = 0; j < input.length; ++j){
            for (int k = 0; k < input[j].length; ++k){
                channels[j * input[j].length +k] = fromInt(input[j][k]);

                //Swap ARGB to RGBA
                byte temp = channels[j * input[j].length +k][0];
                channels[j * input[j].length +k][0] = channels[j * input[j].length +k][1];
                channels[j * input[j].length +k][1] = channels[j * input[j].length +k][2];
                channels[j * input[j].length +k][2] = channels[j * input[j].length +k][3];
                channels[j * input[j].length +k][3] = temp;
            }
        }
        return channels;
    }

    /**
     * Format a 2-dim byte array where the first dimension is the pixel
     * and the second is the channel to a 2-dim int array where the first
     * dimension is the height and the second is the width
     * @param input (byte[][]) : linear representation of the image
     * @param height (int) - Height of the resulting image
     * @param width (int) - Width of the resulting image
     * @return (int[][]) - the image data
     * @throws AssertionError if the input is null
     * or one of the inner arrays of input is null
     * or input's length differs from width * height
     * or height is invalid
     * or width is invalid
     */
    public static int[][] channelsToImage(byte[][] input, int height, int width){
        assert !((input == null) || (input.length != (width * height)) || (height <= 0) || (width <= 0));
        for (int i = 0; i< input.length; ++i){
            assert !(input[i] == null);
        }
        int[][] image = new int[height][width];
        int row = 0;
        int column = 0;
        byte [][] clonedInput = new byte[input.length][4];
        for (int i = 0; i < input.length; ++i){


            //Swap RGBA to ARGB
            clonedInput[i][3] = input[i][2];
            clonedInput[i][2] = input[i][1];
            clonedInput[i][1] = input[i][0];
            clonedInput[i][0] = input[i][3];

            image[row][column] = toInt(clonedInput[i]);
            column = (column + 1) % width;
            if (column == 0){
                row += 1;
            }
        }
        return image;
    }

}