package cs107;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * "Quite Ok Image" Encoder
 * @apiNote Second task of the 2022 Mini Project
 * @author Hamza REMMAL (hamza.remmal@epfl.ch)
 * @version 1.0
 * @since 1.0
 */
public final class QOIEncoder {

    /**
     * DO NOT CHANGE THIS, MORE ON THAT IN WEEK 7.
     */
    private QOIEncoder(){}

    // ==================================================================================
    // ============================ QUITE OK IMAGE HEADER ===============================
    // ==================================================================================

    /**
     * Generate a "Quite Ok Image" header using the following parameters
     * @param image (Helper.Image) - Image to use
     * @throws AssertionError if the colorspace or the number of channels is corrupted or if the image is null.
     *  (See the "Quite Ok Image" Specification or the handouts of the project for more information)
     * @return (byte[]) - Corresponding "Quite Ok Image" Header
     */
    public static byte[] qoiHeader(Helper.Image image){
        assert image != null && (image.channels() == QOISpecification.RGB || image.channels() == QOISpecification.RGBA) && (image.color_space() == QOISpecification.sRGB || image.color_space() == QOISpecification.ALL);
        byte[] header1 = ArrayUtils.concat(QOISpecification.QOI_MAGIC, ArrayUtils.fromInt(image.data()[0].length), ArrayUtils.fromInt(image.data().length));
        byte[] header2 = ArrayUtils.concat(image.channels(), image.color_space());
        return ArrayUtils.concat(header1, header2);


    }

    // ==================================================================================
    // ============================ ATOMIC ENCODING METHODS =============================
    // ==================================================================================

    /**
     * Encode the given pixel using the QOI_OP_RGB schema
     * @param pixel (byte[]) - The Pixel to encode
     * @throws AssertionError if the pixel's length is not 4
     * @return (byte[]) - Encoding of the pixel using the QOI_OP_RGB schema
     */
    public static byte[] qoiOpRGB(byte[] pixel){
        assert pixel.length == 4;
        byte[] qoiPixel = new byte[4];
        qoiPixel[0] = QOISpecification.QOI_OP_RGB_TAG;
        for (int i = 1; i < 4; ++i){
            qoiPixel[i] = pixel[i - 1];
        }
        return qoiPixel;
    }

    /**
     * Encode the given pixel using the QOI_OP_RGBA schema
     * @param pixel (byte[]) - The pixel to encode
     * @throws AssertionError if the pixel's length is not 4
     * @return (byte[]) Encoding of the pixel using the QOI_OP_RGBA schema
     */
    public static byte[] qoiOpRGBA(byte[] pixel){
        assert pixel.length == 4;
        byte[] qoiPixel = new byte[5];
        qoiPixel[0] = QOISpecification.QOI_OP_RGBA_TAG;
        for (int i = 1; i < 5; ++i){
            qoiPixel[i] = pixel[i - 1];
        }
        return qoiPixel;
    }

    /**
     * Encode the index using the QOI_OP_INDEX schema
     * @param index (byte) - Index of the pixel
     * @throws AssertionError if the index is outside the range of all possible indices
     * @return (byte[]) - Encoding of the index using the QOI_OP_INDEX schema
     */
    public static byte[] qoiOpIndex(byte index){
        assert (index >= 0 && index <= 63);
        return ArrayUtils.wrap(index);
    }

    /**
     * Encode the difference between 2 pixels using the QOI_OP_DIFF schema
     * @param diff (byte[]) - The difference between 2 pixels
     * @throws AssertionError if diff doesn't respect the constraints or diff's length is not 3
     * (See the handout for the constraints)
     * @return (byte[]) - Encoding of the given difference
     */
    public static byte[] qoiOpDiff(byte[] diff){
        assert (diff != null && diff.length == 3);
        for (int i = 0; i < diff.length; ++i){
            assert (diff[i] > -3 && diff[i] < 2);
        }
        return ArrayUtils.wrap((byte) (0b0100_0000 | (0b0011_0000 & ((byte)(diff[0] + 2) << 4)) | (0b0000_1100 & ((byte)(diff[1] + 2) << 2)) | (0b0000_0011 & (byte)(diff[2] + 2))));
    }

    /**
     * Encode the difference between 2 pixels using the QOI_OP_LUMA schema
     * @param diff (byte[]) - The difference between 2 pixels
     * @throws AssertionError if diff doesn't respect the constraints
     * or diff's length is not 3
     * (See the handout for the constraints)
     * @return (byte[]) - Encoding of the given difference
     */
    public static byte[] qoiOpLuma(byte[] diff){

        assert (diff != null && diff.length == 3 && diff[1] > -33 && diff[1] < 32 && diff[0] - diff[1] > -9 && diff[0] - diff[1] < 8 && diff[2] - diff[1] > -9 && diff[2] - diff[1] < 8);
        int drMinusDg = (byte) (diff[0] - diff[1]);
        int dbMinusDg = (byte) (diff[2] - diff[1]);

    byte[] encode = new byte[2];
    encode[0] = (byte) (0b1000_0000 | (byte) (diff[1] + 32));
    encode[1] = (byte) ((0b1111_0000 & ((byte) (drMinusDg + 8) << 4)) | (0b0000_1111 & ((byte) (dbMinusDg + 8))));
    return encode;
    }

    /**
     * Encode the number of similar pixels using the QOI_OP_RUN schema
     * @param count (byte) - Number of similar pixels
     * @throws AssertionError if count is not between 0 (exclusive) and 63 (exclusive)
     * @return (byte[]) - Encoding of count
     */
    public static byte[] qoiOpRun(byte count){
        assert count >= 1 && count <= 62;
        return ArrayUtils.wrap((byte) (0b1100_0000 | (count - 1)));
    }

    // ==================================================================================
    // ============================== GLOBAL ENCODING METHODS  ==========================
    // ==================================================================================

    /**
     * Encode the given image using the "Quite Ok Image" Protocol
     * (See handout for more information about the "Quite Ok Image" protocol)
     * @param image (byte[][]) - Formatted image to encode
     * @return (byte[]) - "Quite Ok Image" representation of the image
     */
    public static byte[] encodeData(byte[][] image){

        assert image != null;
        byte[] prevPixel = QOISpecification.START_PIXEL;
        byte[][] hashTable = new byte[64][4];
        int counter = 0;

        ArrayList<byte[]> encoded = new ArrayList<byte[]>();

        for (int i = 0; i < image.length; ++i){
            assert (image[i] != null && image[i].length == 4);

            if (ArrayUtils.equals(image[i], prevPixel)){
                counter += 1;
                if (counter == 62){
                    encoded.add(qoiOpRun((byte) counter));
                    counter = 0;
                }
                continue;
            }
            if (!ArrayUtils.equals(image[i], prevPixel) && counter > 0){
                encoded.add(qoiOpRun((byte) counter));
                counter = 0;
            }


            if (ArrayUtils.equals(image[i], hashTable[QOISpecification.hash(image[i])])){
                encoded.add(qoiOpIndex(QOISpecification.hash(image[i])));
                prevPixel = image[i];
                continue;
            } else {
                hashTable[QOISpecification.hash(image[i])] = image[i];
            }
            int dr = (byte) (image[i][0] - prevPixel[0]);
            int dg = (byte) (image[i][1] - prevPixel[1]);
            int db = (byte) (image[i][2] - prevPixel[2]);

            boolean sameAlpha = image[i][3] == prevPixel[3];

            if (sameAlpha && (dr > -3) && (dr < 2) && (dg > -3) && (dg < 2) && (db > -3) && (db < 2)){
                byte[] difs = {(byte) dr, (byte) dg, (byte) db};
                encoded.add(qoiOpDiff(difs));
                prevPixel = image[i];
                continue;
            }

            int drMinusDg = (byte) (dr - dg);
            int dbMinusDg = (byte) (db - dg);

            if (sameAlpha && (dg > -33) && (dg < 32) && (drMinusDg  > -9) && (drMinusDg < 8) && (dbMinusDg  > -9) && (dbMinusDg < 8)){
                byte[] difs = {(byte) dr, (byte) dg, (byte) db};
                encoded.add(qoiOpLuma(difs));
                prevPixel = image[i];
                continue;
            }
            if (sameAlpha){
                encoded.add(qoiOpRGB(image[i]));
            } else {
                encoded.add(qoiOpRGBA(image[i]));
            }
            prevPixel = image[i];

        }
        if (counter > 0){
            encoded.add(qoiOpRun((byte) counter));
        }
        byte[] encoded2 = new byte[encoded.size()];
        encoded2 = encoded.get(0);
        for (int i = 1; i < encoded.size(); ++i){
            encoded2 = ArrayUtils.concat(encoded2, encoded.get(i));
        }
        return encoded2;
    }

    /**
     * Creates the representation in memory of the "Quite Ok Image" file.
     * @apiNote THE FILE IS NOT CREATED YET, THIS IS JUST ITS REPRESENTATION.
     * TO CREATE THE FILE, YOU'LL NEED TO CALL Helper::write
     * @param image (Helper.Image) - Image to encode
     * @return (byte[]) - Binary representation of the "Quite Ok File" of the image
     * @throws AssertionError if the image is null
     */
    public static byte[] qoiFile(Helper.Image image){
        assert image != null;
        return ArrayUtils.concat(qoiHeader(image), encodeData(ArrayUtils.imageToChannels(image.data())), QOISpecification.QOI_EOF);
    }

}