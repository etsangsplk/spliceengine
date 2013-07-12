package com.splicemachine.storage;


import com.splicemachine.encoding.MultiFieldEncoder;
import com.splicemachine.storage.index.*;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @author Scott Fines
 *         Created on: 7/5/13
 */
public class EntryEncoder {
    private BitIndex bitIndex;
    /*
     * The threshold at which we compress the data. If the data length is less than
     * this, we don't compress, but if it's greater, we do.
     *
     * This value is chosen so that Snappy Compression will actually accomplish something.
     * Adjust it up or down based on empirical results for the individual compressor
     */
    private static final int DATA_COMPRESSION_THRESHOLD=150;
    /*
     * The bit to indicate whether or not the data has been compressed.
     */
    private static final byte COMPRESSED_DATA_BIT = 0x20;

    private MultiFieldEncoder encoder;

    private EntryEncoder(BitIndex bitIndex){
        this.bitIndex = bitIndex;
        this.encoder = MultiFieldEncoder.create(bitIndex.cardinality());
    }

    public EntryEncoder(int size, AllFullBitIndex allFullBitIndex) {
        this.encoder = MultiFieldEncoder.create(size);
        this.bitIndex = allFullBitIndex;
    }

    public MultiFieldEncoder getEntryEncoder(){
        if(encoder==null)
            encoder = MultiFieldEncoder.create(bitIndex.cardinality());
        return encoder;
    }

    public byte[] encode() throws IOException {
        byte[] finalData = encoder.build();

        byte[] bitData = bitIndex.encode();
//        if(finalData.length>DATA_COMPRESSION_THRESHOLD){
//            finalData = Snappy.compress(finalData);
//            //mark the header bit for compressed data
//            bitData[0] = (byte)(bitData[0] | COMPRESSED_DATA_BIT);
//        }

        byte[] entry = new byte[bitData.length+finalData.length+1];
        System.arraycopy(bitData, 0, entry, 0, bitData.length);
        entry[bitData.length] = 0;
        System.arraycopy(finalData,0,entry,bitData.length+1,finalData.length);

        return entry;
    }

    public void reset(BitSet newIndex,BitSet newLengthFields){
        //save effort if they are the same
        int oldCardinality = bitIndex.cardinality();
        boolean differs=false;
        for(int i=newIndex.nextSetBit(0);i>=0;i=newIndex.nextSetBit(i+1)){
            if(!bitIndex.isSet(i)){
                differs=true;
                break;
            }else if(newLengthFields.get(i)!=bitIndex.isLengthDelimited(i)){
                differs=true;
                break;
            }
        }
        if(differs){
            bitIndex = getBitIndex(newIndex,newLengthFields);
        }

        if(oldCardinality==bitIndex.cardinality())
            encoder.reset();
        else
            encoder = MultiFieldEncoder.create(bitIndex.cardinality());
    }

    public static EntryEncoder create(int numCols, BitSet setCols,BitSet lengthFields){
        //TODO -sf- enable ALL Full indices
//        if(setCols==null||setCols.cardinality()==numCols){
//            /*
//             * This is a special case where we are writing *every* column. In this case, we just
//             * set an indicator flag that tells us to not bother reading the index, because everything
//             * is there.
//             */
//            return new EntryEncoder(numCols,new AllFullBitIndex());
//        }else{

            BitIndex indexToUse = getBitIndex(setCols,lengthFields);
            return new EntryEncoder(indexToUse);
//        }
    }

    private static BitIndex getBitIndex(BitSet setCols,BitSet lengthFields) {
        if(lengthFields==null)lengthFields = new BitSet(); //default to no length-delimited fields
        BitIndex indexToUse = BitIndexing.uncompressedBitMap(setCols,lengthFields);
        //see if we can improve space via compression
        BitIndex denseCompressedBitIndex = BitIndexing.compressedBitMap(setCols,lengthFields); //TODO -sf- correct this
        if(denseCompressedBitIndex.encodedSize() < indexToUse.encodedSize()){
            indexToUse = denseCompressedBitIndex;
        }
        //see if sparse is better
        BitIndex sparseBitMap = BitIndexing.sparseBitMap(setCols,lengthFields);
        if(sparseBitMap.encodedSize()<indexToUse.encodedSize()){
            indexToUse = sparseBitMap;
        }
        return indexToUse;
    }
}

