package cs107;

import static cs107.Helper.Image;
import static cs107.Helper.generateImage;

/**
 * "Quite Ok Image" Decoder
 * @apiNote Third task of the 2022 Mini Project
 * @author Hamza REMMAL (hamza.remmal@epfl.ch)
 * @version 1.0
 * @since 1.0
 */
public final class QOIDecoder {

    /**
     * DO NOT CHANGE THIS, MORE ON THAT IN WEEK 7.
     */
    private QOIDecoder(){}

    // ==================================================================================
    // =========================== QUITE OK IMAGE HEADER ================================
    // ==================================================================================

    /**
     * Extract useful information from the "Quite Ok Image" header
     * @param header (byte[]) - A "Quite Ok Image" header
     * @return (int[]) - Array such as its content is {width, height, channels, color space}
     * @throws AssertionError See handouts section 6.1
     */
    public static int[] decodeHeader(byte[] header){
        assert header != null && header.length == QOISpecification.HEADER_SIZE && ArrayUtils.equals(ArrayUtils.extract(header, 0, 4), QOISpecification.QOI_MAGIC) && (header[12] == QOISpecification.RGB || header[12] == QOISpecification.RGBA) && (header[13] == QOISpecification.ALL || header[13] == QOISpecification.sRGB);
        return new int[]{ArrayUtils.toInt(ArrayUtils.extract(header, 4, 4)), ArrayUtils.toInt(ArrayUtils.extract(header, 8, 4)), header[12], header[13]};
    }

    // ==================================================================================
    // =========================== ATOMIC DECODING METHODS ==============================
    // ==================================================================================

    /**
     * Store the pixel in the buffer and return the number of consumed bytes
     * @param buffer (byte[][]) - Buffer where to store the pixel
     * @param input (byte[]) - Stream of bytes to read from
     * @param alpha (byte) - Alpha component of the pixel
     * @param position (int) - Index in the buffer
     * @param idx (int) - Index in the input
     * @return (int) - The number of consumed bytes
     * @throws AssertionError See handouts section 6.2.1
     */
    public static int decodeQoiOpRGB(byte[][] buffer, byte[] input, byte alpha, int position, int idx){
        assert buffer != null && input != null && position >= 0 && position < buffer.length && idx >= 0 && idx < input.length - 2;
        buffer[position] = ArrayUtils.concat(ArrayUtils.extract(input, idx,  3), ArrayUtils.wrap(alpha));
        return 3;
    }

    /**
     * Store the pixel in the buffer and return the number of consumed bytes
     * @param buffer (byte[][]) - Buffer where to store the pixel
     * @param input (byte[]) - Stream of bytes to read from
     * @param position (int) - Index in the buffer
     * @param idx (int) - Index in the input
     * @return (int) - The number of consumed bytes
     * @throws AssertionError See handouts section 6.2.2
     */
    public static int decodeQoiOpRGBA(byte[][] buffer, byte[] input, int position, int idx){
        assert buffer != null && input != null && position >= 0 && position < buffer.length && idx >= 0 && idx < input.length - 3;
        buffer[position] = ArrayUtils.extract(input, idx, 4);
        return 4;
    }

    /**
     * Create a new pixel following the "QOI_OP_DIFF" schema.
     * @param previousPixel (byte[]) - The previous pixel
     * @param chunk (byte) - A "QOI_OP_DIFF" data chunk
     * @return (byte[]) - The newly created pixel
     * @throws AssertionError See handouts section 6.2.4
     */
    public static byte[] decodeQoiOpDiff(byte[] previousPixel, byte chunk){
        assert previousPixel != null && previousPixel.length == 4 && (chunk & (byte) 0b11_00_00_00) == QOISpecification.QOI_OP_DIFF_TAG;
        byte[] currentPixel = new byte[4];
        // Supposing RGBA encoding
        currentPixel[0] = (byte)(previousPixel[0] + (byte) (((chunk & (byte) 0b00_11_00_00) >> 4) - 2));
        currentPixel[1] = (byte)(previousPixel[1] + (byte) (((chunk & (byte) 0b00_00_11_00) >> 2) - 2));
        currentPixel[2] = (byte)(previousPixel[2] + (byte) ((chunk & (byte) 0b00_00_00_11) - 2));
        currentPixel[3] = previousPixel[3];
        return currentPixel;
    }

    /**
     * Create a new pixel following the "QOI_OP_LUMA" schema
     * @param previousPixel (byte[]) - The previous pixel
     * @param data (byte[]) - A "QOI_OP_LUMA" data chunk
     * @return (byte[]) - The newly created pixel
     * @throws AssertionError See handouts section 6.2.5
     */
    public static byte[] decodeQoiOpLuma(byte[] previousPixel, byte[] data){
        assert previousPixel != null && data != null && previousPixel.length == 4 && (data[0] & (byte) 0b10_00_00_00) == QOISpecification.QOI_OP_LUMA_TAG;
        byte[] currentPixel = new byte[4];
        byte greenDiff = (byte)((byte)(data[0] & (byte) 0b00_11_11_11) - 32);


        byte redDiff = (byte)((byte)((byte)((byte)(data[1] >>> 4) & (byte) 0b00_00_11_11) - 8) +  greenDiff);
        byte blueDiff = (byte)((byte)((byte)(data[1] & (byte) 0b00_00_11_11) - 8) +  greenDiff);


        currentPixel[1] = (byte)(previousPixel[1] + (greenDiff));
        currentPixel[0] = (byte)(previousPixel[0] + (redDiff));
        currentPixel[2] = (byte)(previousPixel[2] + (blueDiff));
        currentPixel[3] = previousPixel[3];

        return currentPixel;

    }

    /**
     * Store the given pixel in the buffer multiple times
     * @param buffer (byte[][]) - Buffer where to store the pixel
     * @param pixel (byte[]) - The pixel to store
     * @param chunk (byte) - a QOI_OP_RUN data chunk
     * @param position (int) - Index in buffer to start writing from
     * @return (int) - number of written pixels in buffer
     * @throws AssertionError See handouts section 6.2.6
     */
    public static int decodeQoiOpRun(byte[][] buffer, byte[] pixel, byte chunk, int position){
        assert buffer != null && position >= 0 && position <= buffer.length && pixel != null && pixel.length == 4;
        int numReps = (chunk & 0b00_11_11_11) + 1;
        assert (position + numReps) <= buffer.length;
        for (int i = 0; i < numReps; ++i){
            buffer[position + i] = pixel;
        }
        return numReps - 1;
    }

    // ==================================================================================
    // ========================= GLOBAL DECODING METHODS ================================
    // ==================================================================================

    /**
     * Decode the given data using the "Quite Ok Image" Protocol
     * @param data (byte[]) - Data to decode
     * @param width (int) - The width of the expected output
     * @param height (int) - The height of the expected output
     * @return (byte[][]) - Decoded "Quite Ok Image"
     * @throws AssertionError See handouts section 6.3
     */
    public static byte[][] decodeData(byte[] data, int width, int height){
        assert data != null && width > 0 && height >0;
        byte[][] decoded = new byte[height * width][4];
        byte[] previousPixel = QOISpecification.START_PIXEL;
        byte[][] hashTable = new byte[64][4];
        int idx = 0;
        int position = 0;
        byte tag = 0;
        hashTable[QOISpecification.hash(previousPixel)] = previousPixel;
        while (idx < data.length){
            if (data[idx] == QOISpecification.QOI_OP_RGB_TAG){
                idx += decodeQoiOpRGB(decoded, data, previousPixel[3], position, idx + 1);
            } else if (data[idx] == QOISpecification.QOI_OP_RGBA_TAG) {
                idx += decodeQoiOpRGBA(decoded, data, position, idx + 1);
            } else{
                tag = maskQoiTag(data[idx]);
                if (tag == maskQoiTag(QOISpecification.QOI_OP_DIFF_TAG)){
                    decoded[position] = decodeQoiOpDiff(previousPixel, data[idx]);
                } else if (tag == maskQoiTag(QOISpecification.QOI_OP_LUMA_TAG)) {
                    decoded[position] = decodeQoiOpLuma(previousPixel, ArrayUtils.extract(data, idx, 2));
                    idx += 1;
                } else if (tag == maskQoiTag(QOISpecification.QOI_OP_RUN_TAG)) {
                    position += decodeQoiOpRun(decoded, previousPixel, data[idx], position);
                } else if (tag == maskQoiTag(QOISpecification.QOI_OP_INDEX_TAG)) {
                    decoded[position] = hashTable[data[idx]];
                }
            }
            idx += 1;
            previousPixel = decoded[position];
            if (!ArrayUtils.equals(previousPixel, hashTable[QOISpecification.hash(previousPixel)])){
                hashTable[QOISpecification.hash(previousPixel)] = previousPixel;
            }
            position += 1;
        }
        assert position == decoded.length;
        return decoded;
    }

    /**
     * Return the first two bits in a byte (unsigned)
     * @param fullByte (byte) - Byte to mask
     * @return (byte) - "Masked" byte
     */

    public static byte maskQoiTag(byte fullByte){
        return (byte)((byte)(fullByte >> 6) & 0b00_00_00_11);
    }

    /**
     * Decode a file using the "Quite Ok Image" Protocol
     * @param content (byte[]) - Content of the file to decode
     * @return (Image) - Decoded image
     * @throws AssertionError if content is null
     */
    public static Image decodeQoiFile(byte[] content){
        assert content != null && ArrayUtils.equals(ArrayUtils.extract(content, content.length - 8, 8), QOISpecification.QOI_EOF);
        int[] headerInfo = decodeHeader(ArrayUtils.extract(content, 0, QOISpecification.HEADER_SIZE));
        int a = 0;
        byte[][] decoded = decodeData(ArrayUtils.extract(content, QOISpecification.HEADER_SIZE, content.length - (QOISpecification.HEADER_SIZE + QOISpecification.QOI_EOF.length)), headerInfo[0], headerInfo[1]);
        int[][] imageArray = ArrayUtils.channelsToImage(decoded, headerInfo[1], headerInfo[0]);
        Image im = generateImage(imageArray, (byte) headerInfo[2], (byte) headerInfo[3]);
        return im;
    }

}