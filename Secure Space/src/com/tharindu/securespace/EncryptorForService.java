package com.tharindu.securespace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import sse.org.bouncycastle.crypto.BufferedBlockCipher;
import sse.org.bouncycastle.crypto.PBEParametersGenerator;
import sse.org.bouncycastle.crypto.digests.SHA1Digest;
import sse.org.bouncycastle.crypto.digests.SHA256Digest;
import sse.org.bouncycastle.crypto.digests.SHA512Digest;
import sse.org.bouncycastle.crypto.engines.AESFastEngine;
import sse.org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import sse.org.bouncycastle.crypto.io.CipherOutputStream;
import sse.org.bouncycastle.crypto.modes.CBCBlockCipher;
import sse.org.bouncycastle.crypto.params.KeyParameter;
import sse.org.bouncycastle.crypto.params.ParametersWithIV;
import sse.org.bouncycastle.crypto.prng.ThreadedSeedGenerator;
import android.os.Handler;
import android.os.Message;

import com.paranoiaworks.unicus.android.sse.FileEncActivity;
import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.StaticApp;
import com.paranoiaworks.unicus.android.sse.misc.CheckCodeParserInputStream;
import com.paranoiaworks.unicus.android.sse.misc.CipherInputStreamPI;
import com.paranoiaworks.unicus.android.sse.misc.CryptFile;
import com.paranoiaworks.unicus.android.sse.misc.EncryptorException;
import com.paranoiaworks.unicus.android.sse.misc.ProgressBarToken;
import com.paranoiaworks.unicus.android.sse.misc.ProgressMessage;
import com.paranoiaworks.unicus.android.sse.nativecode.CipherInputStreamNC;
import com.paranoiaworks.unicus.android.sse.nativecode.CipherOutputStreamNC;
import com.paranoiaworks.unicus.android.sse.nativecode.EncryptorNC;
import com.paranoiaworks.unicus.android.sse.utils.CipherProvider;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import ext.os.utils.Base64;

/**
 * Encryptor Class for Services (avoiding UI interactions) -th 
 * @author Tharindu Wijewardane
 */ 
public class EncryptorForService {
	
	public static final String SSE_FILEHEADER_PREFIX = CryptFile.ENC_FILE_HEADER_PREFIX;
	public static final String ENC_FILE_EXTENSION = CryptFile.ENC_FILE_EXTENSION;
	public static final String ENC_FILE_UNFINISHED_EXTENSION = "tmp";
	public static final String SSE_VERSION = "10";
	private static final int TEXT_HEADERSIZE = 8;
	private static final int BASE_ALGORITHM_CODE_OFFSET = 25;  //+25 zipped
	private static final int BASE_ALGORITHM_CODE = 0;
	private static final String MODE_CODES = "tcabnqmjsldriovwupkhxyezfgTCABNQMJSLDRIOVWUPKHXYEZFG"; // last 2char reserve
	private static final double CRC_TIMECOEF = 0.3;
	
	private Map<Integer, AlgorithmBean> availableAlgorithms = new HashMap<Integer, AlgorithmBean>();
	
	private Map<String, byte[]> keysVault = new HashMap<String, byte[]>();
	private int encryptAlgorithmCode;
	private int decryptAlgorithmCode;
	private boolean lastEncZipped = false;
	private boolean lastDecZipped = false;

    
    /** Encryptor with entered password and Base Algorithm (AES 256) */
    public EncryptorForService(String password) throws NoSuchAlgorithmException, InvalidKeySpecException 
    {
    	this(password, BASE_ALGORITHM_CODE, false);
    }
    
    /** Encryptor with entered password and chosen algorithm */
    public EncryptorForService(String password, int algorithmCode) throws NoSuchAlgorithmException, InvalidKeySpecException 
    {
    	this(password, algorithmCode, false);
    }
    
    /** Encryptor with entered password, chosen algorithm and unicode character allowed */
    public EncryptorForService(String password, int algorithmCode, boolean unicodeAllowed) throws NoSuchAlgorithmException, InvalidKeySpecException 
    {
    	setAvailableAlgorithms();
    	encryptAlgorithmCode = algorithmCode;
    	generatePBKeys448Max(password, unicodeAllowed);
    }
    
    /** Enable Native Code Enc/Dec where available */
    public void enableNativeCodeEngine()
    {
    	Set<Integer> keySet = availableAlgorithms.keySet();
    	Iterator<Integer> keySetIterator = keySet.iterator();
    	EncryptorNC nc = new EncryptorNC();
    	while(keySetIterator.hasNext())
    	{
    		AlgorithmBean abTemp = availableAlgorithms.get(keySetIterator.next());
    		abTemp.nativeCodeAvailable = nc.checkCipher(abTemp.innerCode, abTemp.blockSize/8, abTemp.keySize/8);
    	}
    }    
    
    /** Get Map with Available Algorithms */
    public Map<Integer, AlgorithmBean> getAvailableAlgorithms()
    {
    	return availableAlgorithms;
    }
    
    /** For testing purposes */
    public static EncryptorForService getDefaultTestEncryptor()
    {
    	EncryptorForService defaultEncryptor = null;
		try {
			defaultEncryptor = new EncryptorForService("DEFAULT_zxdfhyjcbtgdse355v6dxvh8");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
    	return defaultEncryptor;
    }
    
    /** Get Hash of current Encryption Key */
    public String getEncKeyHash()
	{
		return getMD5Hash(getEncKey());
	}
	
    /** Get current Encryption Algorithm Code */
    public int getEncryptAlgorithmCode()
	{
		return encryptAlgorithmCode;
	}
    
    /** Get Hash of current Decryption Key */
    public String getDecKeyHash()
	{
		return getMD5Hash(getDecKey());
	}
	
    /** Get current Decryption Algorithm Code */
    public int getDecryptAlgorithmCode()
	{
		return decryptAlgorithmCode;
	}
	
	//+ Other Algorithm Attributes
    public String getEncryptAlgorithmComment()
	{
		String cipher = availableAlgorithms.get(encryptAlgorithmCode).comment;
		return cipher;
	}
	
	public String getDecryptAlgorithmComment()
	{
		String cipher = availableAlgorithms.get(decryptAlgorithmCode).comment;
		return cipher;
	}
	
	public String getEncryptAlgorithmShortComment()
	{
		String cipher = availableAlgorithms.get(encryptAlgorithmCode).shortComment;
		if(isEncNativeCodeAvailable()) cipher += " NC";
		return cipher;
	}
	
	public String getDecryptAlgorithmShortComment()
	{
		String cipher = availableAlgorithms.get(decryptAlgorithmCode).shortComment;
		if(isDecNativeCodeAvailable()) cipher += " NC";
		return cipher;
	}
	//- Other Algorithm Attributes
	

	/** Encrypt Text */
	public synchronized String encryptString (String text) throws UnsupportedEncodingException
	{				
		byte[] charEnc = text.trim().getBytes("UTF8");	
		byte[] tempEnc = Helpers.concat(getChecksumHeader(charEnc), charEnc);
		byte[] encrypted = encrypt(zipByteArray(tempEnc, true), getEncKey());
		String ac = getOneCharCode(encryptAlgorithmCode, lastEncZipped);
	    Base64 base = new Base64(true); 
	    String output = base.encodeToString(encrypted).replaceAll("-", "!") + ac;
		
		return output;
	}
	
	/** Decrypt Text */
	public synchronized String decryptString (String text) throws Exception
	{
		String output = "-1";
		byte[] input = text.trim().replaceAll("!", "-").getBytes();
		setDecryptAlgorithmCodefromOneCharCode((input[input.length - 1]));
		input = Helpers.getSubarray(input, 0, input.length - 1);		
		Base64 base = new Base64(true);
	    byte[] encrypted = base.decode(input);
	    byte[] bOutput = unzipByteArray(decrypt(encrypted, getDecKey()), true);
	    if (checkMessageIntegrity(bOutput))
	    {
	    	bOutput = Helpers.getSubarray(bOutput, TEXT_HEADERSIZE, bOutput.length - TEXT_HEADERSIZE);
	    	output = new String(bOutput, "UTF8");
	    }
	    	else throw new DataFormatException("Incorrect checksum");
	    return output;
	}
	
	/** Encrypt byte array */
	public byte[] encrypt(byte input[], boolean compress)
    {
        if(compress) return encrypt(zipByteArray(input, false), getEncKey());
        else return encrypt(input, getEncKey());
    }

	/** Decrypt byte array */
	public byte[] decrypt(byte input[], boolean decompress) throws Exception
    {
    	if(decompress) return unzipByteArray(decrypt(input, getDecKey()), false);
    	else return decrypt(input, getDecKey());
    }

	/** Encrypt byte array and attach provided crc */
	public byte[] encryptWithCRC(byte input[], String crc)
    {
		byte[] innerHeader = new byte[64];
		byte[] toEncrypt = new byte[input.length + innerHeader.length];
		innerHeader = Helpers.concat(getRandomBA(), crc.getBytes());
		toEncrypt = Helpers.concat(innerHeader, input);
		innerHeader = null; input = null;		
		byte[] encOutput = encrypt(toEncrypt, getEncKey());		
		toEncrypt = null;		
		byte[] output = Helpers.concat(getOneCharCode(encryptAlgorithmCode, true).getBytes(), encOutput);   	
    	   	
    	return output;
    }

	/** Decrypt byte array and add crc to "StringBuffer crc" */
	public byte[] decryptWithCRC(byte input[], StringBuffer crc) throws Exception
    {
    	setDecryptAlgorithmCodefromOneCharCode(input[0]);
    	
    	byte[] decOutput = decrypt(Helpers.getSubarray(input, 1, input.length - 1), getDecKey());
    	input = null;
    	crc.append(new String(Helpers.getSubarray(decOutput, 32, 32)));
    	
    	return Helpers.getSubarray(decOutput, 64, decOutput.length - 64);
    }
	
	/** Get MD5 of text in String */
	public static String getMD5Hash (String text)
	{
		return getMD5Hash(text.getBytes());
	}
	
	/** Get MD5 of Byte Array */
	public static String getMD5Hash (byte[] text)
	{
		MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	    m.update(text, 0, text.length);
	    String hash = new BigInteger(1, m.digest()).toString(16);
		while (hash.length() < 32) hash = "0" + hash;
		return hash.toLowerCase();
	}
    
	/** Get Short (4B) hash of Byte Array */
	public static byte[] getShortHash (byte[] data)
	{
		int pieceSize = 4;
		String md5s = getMD5Hash(data).toUpperCase();
		byte[] md5bin = new byte[16];
		List<byte[]> hashPiece = new ArrayList<byte[]>();
		
		for(int i = 0; i < md5s.length(); i += 2)
		{
			md5bin[i / 2] = Integer.valueOf(md5s.substring(i, i + 2), 16).byteValue();
		}
				
		for(int i = 0; i < md5bin.length; i += pieceSize)
		{
			byte[] temp = new byte[pieceSize];
				for(int j = 0; j < pieceSize; ++j)
				{
					temp[j] = md5bin[j + i];
				}
			hashPiece.add(temp);
		}
		
		byte[] outputBytes = hashPiece.get(0);
		for (int i = 1; i < hashPiece.size(); ++i)
		{
			outputBytes = Helpers.xorit(outputBytes, hashPiece.get(i));
		}
		
		return outputBytes;
	}
	
	/** Get SHA256 of Byte Array */
	public static byte[] getSHA256Hash(byte [] text)
    {
    	byte[] hash = new byte[32];
    	SHA256Digest digester = new SHA256Digest();
    	digester.update(text, 0, text.length);
    	digester.doFinal(hash, 0);
    	return hash;
    }
    
	/** Get SHA512 of Byte Array */
    public static byte[] getSHA512Hash(byte [] text)
    {
    	byte[] hash = new byte[64];
    	SHA512Digest digester = new SHA512Digest();
    	digester.update(text, 0, text.length);
    	digester.doFinal(hash, 0);
    	return hash;
    }
	
	/** Encrypt Byte Array - Execution */
	private byte[] encrypt(byte[] inputText, byte[] key)
    {  	
    	byte[] output = null; 
	    
		try {
			int bytesProcessed;
			
			BufferedBlockCipher cipher = CipherProvider.getBufferedBlockCipher(true, getEncIVBytes(), key, getEncryptAlgorithmCode());
			output = new byte[cipher.getOutputSize(inputText.length)];
			bytesProcessed = cipher.processBytes(inputText, 0, inputText.length, output, 0);
			bytesProcessed = cipher.doFinal(output, bytesProcessed);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
    	return output;
    }
    
	/** Decrypt Byte Array - Execution */
	private byte[] decrypt(byte[] inputText, byte[] key) throws Exception
    {  	
    	byte[] output = null;

		try {
			int length = 0;
			int bytesProcessed;
			
			BufferedBlockCipher cipher = CipherProvider.getBufferedBlockCipher(false, getDecIVBytes(), key, getDecryptAlgorithmCode());
			byte[] buffer = new byte[cipher.getOutputSize(inputText.length)];
			bytesProcessed = cipher.processBytes(inputText, 0, inputText.length, buffer, 0);
			length += bytesProcessed;
			bytesProcessed = cipher.doFinal(buffer, length);
			length += bytesProcessed;
			
	  	    output = new byte[length];
	  	    System.arraycopy(buffer, 0, output, 0, length);

		} catch (Exception e) {
			throw e;
		} 

		return output;
    }
    
	/** Compress Byte Array */
	public synchronized byte[] zipByteArray(byte[] input)
	{	
		return zipByteArray(input, false);
	}
	
	/** Compress Byte Array
	 *  note: checkLenght (if true - try to compress, but if longer then original return original input )
	 */
	private byte[] zipByteArray(byte[] input, boolean checkLenght)
	{				
		Deflater compressor = new Deflater();
	    compressor.setLevel(Deflater.BEST_COMPRESSION);    
	    compressor.setInput(input);
	    compressor.finish();
	    
	    ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
	    
	    byte[] buf = new byte[4096];
	    while (!compressor.finished()) {
	        int count = compressor.deflate(buf);
	        bos.write(buf, 0, count);
	    }
	    try {
	        bos.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    
	    byte[] compressedData = bos.toByteArray();     
	    
	    if ((compressedData.length > input.length) && checkLenght) 
	    {	
	    	lastEncZipped = false;
	    	return input;
	    }
	    
	    lastEncZipped = true;
	    return compressedData;		
	}
	
	/** Decompress Byte Array */
	public synchronized byte[] unzipByteArray(byte[] input)
	{	
		return unzipByteArray(input, false);
	}
	
	/** Decompress Byte Array, if checkLenght is true - only if necessary */
	private byte[] unzipByteArray(byte[] compressedData, boolean checkLenght)
    {    	
       	if(!lastDecZipped && checkLenght) return compressedData;
    	Inflater decompressor = new Inflater();
        decompressor.setInput(compressedData);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);

        byte[] buf = new byte[4096];
        try {
			while (!decompressor.finished()) 
			{
				int count = decompressor.inflate(buf);
				if (count > 0) bos.write(buf, 0, count);
					else if (count == 0 && decompressor.finished()) break;
						else throw new DataFormatException("Bad datasize: " + compressedData.length);
			}
			bos.close();		
		} catch (DataFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        byte[] decompressedData = bos.toByteArray();       
        return decompressedData;
    }
    
    /** Compress Object */
	public static byte[] zipObject(Object inputObject, StringBuffer outputChecksum) throws IOException
    {
    	long processTime = System.currentTimeMillis();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		CheckedOutputStream csumo = new CheckedOutputStream (baos, new CRC32());
	    GZIPOutputStream gzos = new GZIPOutputStream(csumo);
	    ObjectOutputStream oos = new ObjectOutputStream(gzos);

		oos.writeObject(inputObject);			
		oos.flush();
		oos.close();
		
		if(outputChecksum != null) outputChecksum.append(getMD5Hash(Long.toString(csumo.getChecksum().getValue())));
		processTime = System.currentTimeMillis() - processTime;
		return baos.toByteArray();
    }    
   
    /** Decompress Object */
    public static Object unzipObject(byte[] zippedObject, StringBuffer outputChecksum) throws IOException, ClassNotFoundException
    {
    	long processTime = System.currentTimeMillis();
    	
	    ByteArrayInputStream bais = new ByteArrayInputStream(zippedObject);
	    CheckedInputStream cis = new CheckedInputStream(bais, new CRC32());
	    GZIPInputStream gzis = new GZIPInputStream(cis);
	    ObjectInputStream ois = new ObjectInputStream(gzis);
	    
	    Object object = ois.readObject();
		ois.close();
		
		if(outputChecksum != null) outputChecksum.append(getMD5Hash(Long.toString(cis.getChecksum().getValue())));
		processTime = System.currentTimeMillis() - processTime;
		return object;
    }
	
    /** Compress and Encrypt File/Directory */
	public synchronized long zipAndEncryptFile(File inputFile, boolean compress, ProgressBarToken progressBarToken) throws IOException, InterruptedException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException, DataFormatException 
    {
    	long processTime = Calendar.getInstance().getTimeInMillis();
    	boolean nativeCode = isEncNativeCodeAvailable();
		
//		Handler progressHandler = progressBarToken.getProgressHandler();
//		progressHandler.sendMessage(Message.obtain(progressHandler, -1011)); 
//        progressHandler.sendMessage(Message.obtain(progressHandler, 0));
    	
        final int BUFFER = 65536;
		
		lastEncZipped = true;
		
		File outputFile = new File(inputFile.getParent() + File.separator + inputFile.getName() + "." + ENC_FILE_EXTENSION + "." + ENC_FILE_UNFINISHED_EXTENSION);
		String outputFileFinalName = outputFile.getAbsolutePath().substring(0, outputFile.getAbsolutePath().length() - (ENC_FILE_UNFINISHED_EXTENSION.length() + 1));
		File outputFileFinal = new File(outputFileFinalName);
		if(outputFileFinal.exists() && !outputFileFinal.delete()) throw new IOException("ENC File Delete: Failed");
		
		// FileOutputStream
		FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
//		progressHandler.sendMessage(Message.obtain(progressHandler, 
//				FileEncActivity.FEA_PROGRESSHANDLER_SET_OUTPUTFILEPATH, outputFileFinal.getAbsolutePath()));
		
    	// CipherOutputStream		
		OutputStream cipherOutputStream = null;
		if(nativeCode) {
			cipherOutputStream = new CipherOutputStreamNC(new BufferedOutputStream(fileOutputStream, BUFFER), getEncIVBytes(), getEncKey(), encryptAlgorithmCode);
		}
		else {
			BufferedBlockCipher cipher = CipherProvider.getBufferedBlockCipher(true, getEncIVBytes(), getEncKey(), getEncryptAlgorithmCode());
			cipherOutputStream = new CipherOutputStream(new BufferedOutputStream(fileOutputStream, BUFFER), cipher);
		}
		
		// ZipOutputStream
		ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(cipherOutputStream);
		if(compress)zipOutputStream.setMethod(ZipArchiveOutputStream.DEFLATED);
		else zipOutputStream.setMethod(ZipArchiveOutputStream.STORED);
		zipOutputStream.setUseZip64(Zip64Mode.AsNeeded);
				
		// Write File Header
		fileOutputStream.write((SSE_FILEHEADER_PREFIX + SSE_VERSION + getOneCharCode(encryptAlgorithmCode, lastEncZipped) + "..").getBytes());
		
		// Write Encrypted Header
		byte[] random64 = getRandomBA(true, false);
		byte[] random32 = Helpers.getSubarray(random64, 0, 32);
		byte[] checkcode = getMD5Hash(Helpers.getSubarray(random64, 32, 32)).getBytes();
		cipherOutputStream.write(random32); // random prefix (32B)
		cipherOutputStream.write(checkcode); // checkcode (32B)
		
		if(inputFile.isFile()) {
			zipSingleFile(inputFile, zipOutputStream, BUFFER, compress, null);
		}
		else if(inputFile.isDirectory()) {
			long sizeCounter = this.zipDir(inputFile, zipOutputStream, BUFFER, compress, null);
			//Log.v("DIRSIZE: ", "" + sizeCounter + " : " + Helpers.getDirectorySize(inputFile));
		}
		
		//zipOutputStream.flush();
		zipOutputStream.finish();
		cipherOutputStream.write(checkcode); // end checkcode (32B)
		if(nativeCode)((CipherOutputStreamNC)cipherOutputStream).doFinal(); // last buffer + padding
		
		cipherOutputStream.flush();
		fileOutputStream.flush();
		zipOutputStream.close();
		
		processTime = Calendar.getInstance().getTimeInMillis() - processTime;
		
		// Rename Output File tmp to final name 
		outputFile.renameTo(outputFileFinal);
		
		return processTime;
    }
   
	/** Extension of zipAndEncryptFile Method */
	private void zipSingleFile(File inputFile, ZipArchiveOutputStream zos, final int BUFFER, boolean compress, Handler progressHandler) throws IOException, InterruptedException
	{
		byte data[] = new byte[BUFFER];
		ProgressMessage hm = new ProgressMessage();
        if(compress)hm.setFullSize(inputFile.length());
        else hm.setFullSize((long)((CRC_TIMECOEF + 1) * inputFile.length()));
        
		ZipArchiveEntry entry = new ZipArchiveEntry(inputFile.getName());         
		if(!compress) // STORED ONLY
		{
			entry.setCompressedSize(inputFile.length());
            entry.setSize(inputFile.length());
            entry.setCrc(getCRC32(inputFile));	//used overloaded other method to avoid progress bar -th
		}
		entry.setTime(inputFile.lastModified());
		
		FileInputStream in = new FileInputStream(inputFile);
		BufferedInputStream origin = new BufferedInputStream(in, BUFFER);		
		zos.putArchiveEntry(entry);

		int count;
		while((count = origin.read(data, 0, BUFFER)) != -1) {
			zos.write(data, 0, count);
//			
//			hm.setProgressAbs(hm.getProgressAbs() + count);
//			if(!hm.isRelSameAsLast()) progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
			checkThreadInterruption();
		}
		origin.close();
		zos.closeArchiveEntry();
//		return hm.getProgressAbs();
	}
	
	/** Extension of zipAndEncryptFile Method */
    private long zipDir(File dir, ZipArchiveOutputStream zos, final int BUFFER, boolean compress, Handler progressHandler) throws IOException, InterruptedException, DataFormatException
    {
    	String originalPath = dir.getAbsolutePath().replace(dir.getName(), "");
    	ProgressMessage hm = new ProgressMessage();
        if(compress)hm.setFullSize(Helpers.getDirectorySize(dir));
        else hm.setFullSize((long)((CRC_TIMECOEF + 1) * Helpers.getDirectorySize(dir)));
    	if(hm.getFullSize() == 0) throw new DataFormatException("Selected Folder size is 0");
    	hm.setProgressAbs(0);
    	return zipDir(dir, zos, BUFFER, originalPath, compress, hm, progressHandler);
    }
    
    /** Extension of zipAndEncryptFile Method */
    private long zipDir(File dir, ZipArchiveOutputStream zos, final int BUFFER, String originalPath, boolean compress, ProgressMessage hm, Handler progressHandler) throws IOException, InterruptedException 
	{ 
		String[] dirList = dir.list();
		byte[] readBuffer = new byte[BUFFER]; 
		int bytesIn = 0; 
		
		for(int i = 0; i < dirList.length; i++)
		{ 
			File f = new File(dir, dirList[i]);
			
			String tempPath = f.getAbsolutePath().substring(originalPath.length(), f.getAbsolutePath().length());
			ZipArchiveEntry anEntry = null;
			
			if(f.isDirectory()) 
	        { 
				anEntry = new ZipArchiveEntry(tempPath + File.separator);
				if(!compress) // STORED ONLY
				{
					anEntry.setSize(0);				
					anEntry.setCrc(0);
				}
				anEntry.setTime(f.lastModified());			
				zos.putArchiveEntry(anEntry);
				zos.closeArchiveEntry();
				zipDir(f, zos, BUFFER, originalPath, compress, hm, progressHandler);
	            continue; 
	        }
			anEntry = new ZipArchiveEntry(tempPath);
			
			anEntry.setSize(f.length()); // needed for unziped size
			anEntry.setTime(f.lastModified());
			if(!compress) // STORED ONLY
			{
				anEntry.setCompressedSize(f.length());;
				anEntry.setCrc(getCRC32(f, progressHandler, hm));
			}
	        zos.putArchiveEntry(anEntry); 
			
			FileInputStream fis = new FileInputStream(f);
			BufferedInputStream origin = new BufferedInputStream(fis, BUFFER);	


	        while((bytesIn = origin.read(readBuffer)) != -1) 
	        {        	
	        	zos.write(readBuffer, 0, bytesIn);
	        	hm.setProgressAbs(hm.getProgressAbs() + bytesIn);
	        	
	        	if(!hm.isRelSameAsLast()) progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
	        	checkThreadInterruption();
	        } 
	        zos.closeArchiveEntry();
	        origin.close();
	        fis.close();
	    }
		return hm.getProgressAbs();
	}
    
    /** Decompress and Decrypt File (one pass) */
    public synchronized long unzipAndDecryptFile(File inputFile, ProgressBarToken progressBarToken) throws IOException, InterruptedException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException, DataFormatException, EncryptorException
    {
    	long processTime = Calendar.getInstance().getTimeInMillis();
    	
//    	Handler progressHandler = progressBarToken.getProgressHandler();
//        progressHandler.sendMessage(Message.obtain(progressHandler, 0));
        
        ProgressMessage hm = new ProgressMessage();
    	hm.setProgressAbs(0);
    	
		long tempSizeCounter = 0;
		int fileCounter = 0;
		int dirCounter = 0;
		String outputFilePath = null;
		boolean onlyOneFileArchive = false;
		List<String> integrityErrorList = new ArrayList<String>();
    	
    	final int BUFFER = 65536;
		byte buffer[] = new byte[BUFFER];
    	final int CHECKCODE_SIZE = 32;
    	final int HEADER_SIZE = 8;
		byte preambleBuffer[] = new byte[HEADER_SIZE];
		byte checkCode[] = new byte[CHECKCODE_SIZE];
		byte checkCodeEnd[] = new byte[CHECKCODE_SIZE];
		byte randomPrefix[] = new byte[CHECKCODE_SIZE];

        hm.setFullSize(inputFile.length());
					    
        // FileInputStream
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        fileInputStream.read(preambleBuffer);	
		String preamble = new String(preambleBuffer);
		String codingMethod = preamble.substring(5, 6);	    	    

    	// CipherInputStream
		setDecryptAlgorithmCodefromOneCharCode((byte)codingMethod.charAt(0));
		boolean nativeCode = isDecNativeCodeAvailable();
//    	progressHandler.sendMessage(Message.obtain(progressHandler, -1004));
		BufferedInputStream cipherInputStream = null;
		if (nativeCode)
			cipherInputStream = new BufferedInputStream(new CipherInputStreamNC(fileInputStream, getDecIVBytes(), getDecKey(), getDecryptAlgorithmCode()), BUFFER * 2);
		else
			cipherInputStream = new BufferedInputStream(new CipherInputStreamPI(fileInputStream, getDecIVBytes(), getDecKey(), getDecryptAlgorithmCode()), BUFFER * 2);
    	
    	cipherInputStream.read(randomPrefix); // remove random prefix
    	cipherInputStream.read(checkCode);
    	if(Helpers.regexGetCountOf(checkCode, "[^a-z0-9]") > 0) 
    		throw new DataFormatException((new Integer(R.string.common_error_invalid_password_file_text)).toString());
	    
    	// CheckCodeParserInputStream
    	CheckCodeParserInputStream checkCodeParserInputStream = new CheckCodeParserInputStream(
    			cipherInputStream, inputFile.length() - HEADER_SIZE - (2 * CHECKCODE_SIZE), true);
	    
    	// ZipArchiveInputStream
    	ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(checkCodeParserInputStream);
	    
		String sOutputFolder = inputFile.getParentFile().getAbsolutePath();
		ZipArchiveEntry ze = zipArchiveInputStream.getNextZipEntry();
		String postFix = "";
		String firstDir = "";
		if(ze.getName().indexOf(File.separator) > -1)  // directory/file archive
		{
			postFix = getFilePostfix(ze.getName(), sOutputFolder);
			firstDir = Helpers.getFirstDirFromFilepath(ze.getName());
//			progressHandler.sendMessage(Message.obtain(progressHandler, FileEncActivity.FEA_PROGRESSHANDLER_SET_OUTPUTFILEPATH, 
//					sOutputFolder + File.separator + firstDir + (postFix == null ? "" : postFix)));			
		}
		else
		{
			outputFilePath = inputFile.getParent() + File.separator + ze.getName();
			postFix = getFilePostfix(outputFilePath, null);
			if(postFix != null) outputFilePath = Helpers.replaceLastDot(outputFilePath, postFix);
//			progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), 
//					FileEncActivity.FEA_PROGRESSHANDLER_SET_OUTPUTFILEPATH, outputFilePath));
			onlyOneFileArchive = true;
		}

		while (ze != null) 
		{
			if(!onlyOneFileArchive)
			{
				String fileName = ze.getName();
				if(postFix != null) fileName = fileName.replaceFirst(Pattern.quote(firstDir), firstDir + postFix);
				outputFilePath = sOutputFolder + File.separator + fileName;
			}
			
			if(outputFilePath.endsWith(File.separator)) // directory
			{
				File tempDir = new File(outputFilePath);
				tempDir.mkdirs();
				ze = zipArchiveInputStream.getNextZipEntry();
				++dirCounter;
				continue;
			}
			++fileCounter;
			
			//SSElog.d("outputFilePath A", outputFilePath + " : " + postFix + " : " + fileCounter);
			
			File newFile = new File(outputFilePath);
			new File(newFile.getParent()).mkdirs();

			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(newFile), BUFFER);

			CRC32 crc = new CRC32();
			crc.reset();
			int bytesIn;
			while ((bytesIn = zipArchiveInputStream.read(buffer)) > 0)
			{
				os.write(buffer, 0, bytesIn);
				crc.update(buffer, 0, bytesIn);
				long size = zipArchiveInputStream.getBytesRead();
				
	            if (size - tempSizeCounter > BUFFER)
	            {
	            	hm.setProgressAbs(size);
//	            	if(!hm.isRelSameAsLast()) progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
		        	checkThreadInterruption(outputFilePath); // check and prepare path for wiping
		        	tempSizeCounter = size;
	            }
			}

			os.flush();
			os.close();
			ZipArchiveEntry zeLast = ze;
			ze = zipArchiveInputStream.getNextZipEntry();
			//System.out.println("\nCRC: " + crc.getValue() + " : " + zeLast.getCrc());
			if(zeLast.getTime() > -1) newFile.setLastModified(zeLast.getTime());
			if(crc.getValue() != zeLast.getCrc()) integrityErrorList.add(zeLast.getName());
			zeLast = null;
		}
		
		checkCodeEnd = checkCodeParserInputStream.getCheckCode();
		
		hm.set100();
		zipArchiveInputStream.close();
		cipherInputStream.close();
		fileInputStream.close();
		
		if(integrityErrorList.size() > 0)
		{
			StringBuffer exceptionText = new StringBuffer();
			exceptionText.append("<b>" + StaticApp.getStringResource("fe_integrity_error") + "</b><br/>");
			for(int i = 0; i < integrityErrorList.size(); ++i)
			{
				exceptionText.append("- " + integrityErrorList.get(i) + "<br/>");
			}
			throw new EncryptorException(exceptionText.toString());
		}
		
		//SSElog.d("checkCode", "" + new String(checkCode) + " : " + new String(checkCodeEnd));
    	if(!(new String(checkCode).equals(new String(checkCodeEnd))))
    		throw new DataFormatException((new Integer(R.string.common_error_invalid_checksum_file_text)).toString());
		
		processTime = Calendar.getInstance().getTimeInMillis() - processTime;
		return processTime;
    }  
        
    /** Generate password-base Keys (128, 256, 448 bits) */
    private void generatePBKeys448Max(String pw, boolean unicodeAllowed) throws NoSuchAlgorithmException, InvalidKeySpecException 
	{
		pw = pw.trim();
		if(unicodeAllowed) pw = convertToCodePoints(pw);

    	byte[] shaL1 = getSHA512Hash(pw.getBytes());   	
    	byte[] shaSalt = getSHA256Hash(Helpers.getSubarray(shaL1, 0, 8));
    	byte[] shaIV = getSHA256Hash(Helpers.getSubarray(shaL1, 8, 8));
    	
		PKCS12ParametersGenerator pGen = new PKCS12ParametersGenerator(new SHA1Digest());
		char[] passwordChars = pw.toCharArray();
		final byte[] pkcs12PasswordBytes = PBEParametersGenerator.PKCS12PasswordToBytes(passwordChars);
		pGen.init(pkcs12PasswordBytes, shaSalt, 600);
		CBCBlockCipher aesCBC = new CBCBlockCipher(new AESFastEngine());
		ParametersWithIV aesCBCParams = (ParametersWithIV) pGen.generateDerivedParameters(256, 128);
		aesCBC.init(false, aesCBCParams);		
		byte[] key = ((KeyParameter)aesCBCParams.getParameters()).getKey();
		
		byte[] k01 = Helpers.getSubarray(key, 0, 16);
		byte[] k02 = Helpers.getSubarray(key, 16, 16);
		keysVault.put("KS256", key);
		keysVault.put("KS128", Helpers.xorit(k01, k02));
		
		byte[] k31 = Helpers.getSubarray(shaL1, 40, 24);
		byte[] k32 = Helpers.concat(key, k31);
		keysVault.put("KS448", k32);
		
		String forIV = new String(getMD5Hash(shaIV));	
		PKCS12ParametersGenerator pGenIV = new PKCS12ParametersGenerator(new SHA1Digest());
		char[] ivChars = forIV.toCharArray();
		final byte[] pkcs12IVBytes = PBEParametersGenerator.PKCS12PasswordToBytes(ivChars);
		pGenIV.init(pkcs12IVBytes, getMD5Hash(shaSalt).getBytes(), 100);
		CBCBlockCipher aesCBC2 = new CBCBlockCipher(new AESFastEngine());
		ParametersWithIV aesCBCParams2 = (ParametersWithIV) pGenIV.generateDerivedParameters(256, 128);
		aesCBC2.init(false, aesCBCParams2);		
		byte[] keyIV = ((KeyParameter)aesCBCParams2.getParameters()).getKey();		
		
		byte[] k11 = Helpers.getSubarray(keyIV, 0, 16);
		byte[] k12 = Helpers.getSubarray(keyIV, 16, 16);
		byte[] ivTemp = Helpers.xorit(k11, k12);
		byte[] k21 = Helpers.getSubarray(ivTemp, 0, 8);
		byte[] k22 = Helpers.getSubarray(ivTemp, 8, 8);
		keysVault.put("IS128", ivTemp);
		keysVault.put("IS64", Helpers.xorit(k21, k22));
	 }

	//+ private attributes getters
    private String getEncCipherString()
	{
		String cipher = availableAlgorithms.get(encryptAlgorithmCode).code;
		return cipher;
	}
	
	private String getDecCipherString()
	{
		String cipher = availableAlgorithms.get(decryptAlgorithmCode).code;
		return cipher;
	}
	
	private String getEncCipherProvider()
	{
		String cipher = availableAlgorithms.get(encryptAlgorithmCode).provider;
		return cipher;
	}
	
	private String getDecCipherProvider()
	{
		String cipher = availableAlgorithms.get(decryptAlgorithmCode).provider;
		return cipher;
	}
	
	private byte[] getEncKey()
	{
		int size = availableAlgorithms.get(encryptAlgorithmCode).keySize;
		String kCode = "KS" + Integer.toString(size);
		byte[] key = keysVault.get(kCode);
		return key;
	}
	
	private byte[] getDecKey()
	{
		int size = availableAlgorithms.get(decryptAlgorithmCode).keySize;
		String kCode = "KS" + Integer.toString(size);
		byte[] key = keysVault.get(kCode);
		return key;
	}
	
	private byte[] getEncIVBytes()
	{
		int size = availableAlgorithms.get(encryptAlgorithmCode).blockSize;
		String ivCode = "IS" + Integer.toString(size);
		byte[] iv = keysVault.get(ivCode);
		return iv;
	}
	
	private byte[] getDecIVBytes()
	{
		int size = availableAlgorithms.get(decryptAlgorithmCode).blockSize;
		String ivCode = "IS" + Integer.toString(size);
		byte[] iv = keysVault.get(ivCode);
		return iv;
	}
	
	private boolean isEncNativeCodeAvailable()
	{
		return availableAlgorithms.get(encryptAlgorithmCode).nativeCodeAvailable;
	}
	
	private boolean isDecNativeCodeAvailable()
	{
		return availableAlgorithms.get(decryptAlgorithmCode).nativeCodeAvailable;
	}
	
	private String getOneCharCode(int ac, boolean zipped)
	{
		int tempEAC = ac;
		if(zipped) tempEAC += BASE_ALGORITHM_CODE_OFFSET;
		return Character.toString(MODE_CODES.charAt(tempEAC));
	}
	//- private attributes getters
	
	/** Convert "Algorithm OneCharCode" (used in the "sse" encrypted texts and files) to Algorithm Code and set it as current*/
	private void setDecryptAlgorithmCodefromOneCharCode(byte ch) throws NoSuchAlgorithmException
	{
		int c = MODE_CODES.indexOf(ch);
		if (c >= BASE_ALGORITHM_CODE_OFFSET)
		{
			lastDecZipped = true;
			decryptAlgorithmCode = c - BASE_ALGORITHM_CODE_OFFSET;
		}
		else 
		{
			lastDecZipped = false;
			decryptAlgorithmCode = c;
		}
		
		AlgorithmBean testAb = getAvailableAlgorithms().get((Integer)decryptAlgorithmCode);
		if(testAb == null) throw new NoSuchAlgorithmException();	
	}
	
	/** Get 2B checksum + 2B random */
	private static byte[] getChecksumHeader (byte[] text)
	{	
		byte[] output = new byte[TEXT_HEADERSIZE];
		byte[] random = getRandomBA();
		byte[] checksum = getShortHash(text);
		output = Helpers.getSubarray(random, 0, TEXT_HEADERSIZE);
		output[1] = checksum[0]; output[3] = checksum[2];
		return output;
	}
	
	/** Verify checksum */
	private static boolean checkMessageIntegrity (byte[] bOutput)
	{	
	    boolean t = false;
		byte[] checksumOrg = Helpers.getSubarray(bOutput, 0, TEXT_HEADERSIZE);
	    byte[] checksumCur = getChecksumHeader(Helpers.getSubarray(bOutput, TEXT_HEADERSIZE, bOutput.length - TEXT_HEADERSIZE));
	    if ((checksumOrg[1] == checksumCur[1]) && (checksumOrg[3] == checksumCur[3]))
	    	t = true;
	    return t;
	}
	
	protected static byte[] getRandomBA()
	{
		return getRandomBA(false, true);
	}
	
	/** Get Random bytes using ThreadedSeedGenerator */
	private static byte[] getRandomBA(boolean bytes64, boolean fast)
	{	
		ThreadedSeedGenerator tsg = new ThreadedSeedGenerator();
		byte[] tsgOutput = tsg.generateSeed(64, fast);
		byte[] timeOutput = String.valueOf(System.currentTimeMillis()).getBytes();
		byte[] seed = Helpers.concat(tsgOutput, timeOutput);
		byte[] output = null;
		
		try {
			SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
			rand.setSeed(seed);		
			byte[]randomNum = new byte[64];
			rand.nextBytes(randomNum);
			output = bytes64 ? getSHA512Hash(randomNum) : getSHA256Hash(randomNum);
		} catch (Exception e1) {
			e1.printStackTrace();
		}  

		return output;
	}	
	
	/** Fill availableAlgorithms Map with Available Algorithms */
	private void setAvailableAlgorithms()
	{
		List<AlgorithmBean> algorithms = algorithmsCodeBook();
		for (int i = 0; i < algorithms.size(); ++i)
		{
			AlgorithmBean ab = algorithms.get(i);
			availableAlgorithms.put(ab.innerCode, ab);
		}
	}
	
	/** Get proper File Name for File Encryptor output files */
	private static String getFilePostfix(String origPath, String prefixDir)
	{
		String postFix = null;
		if(prefixDir != null)
		{
			if((new File(prefixDir + File.separator + origPath)).exists())
			{
				for(int i = 1; i < 1000; ++i)
				{
					String tempPF = "_(" + i + ")";
					String firstDir = Helpers.getFirstDirFromFilepath(origPath);
					String tempPath = prefixDir + File.separator + origPath.substring(0, origPath.indexOf(firstDir)) + firstDir + tempPF;
					if((new File(tempPath)).exists()) continue;
					postFix = tempPF;
					break;
				}			
			}
		} 
		else 
		{
			if((new File(origPath)).exists())
			{
				for(int i = 1; i < 1000; ++i)
				{
					String tempPF = "_(" + i + ").";
					String tempPath = Helpers.replaceLastDot(origPath, tempPF);
					if((new File(tempPath)).exists()) continue;
					postFix = tempPF;
					break;
				}			
			}
		}
		return postFix;
	}
	
    /** Convert text to "char + unicode int representation string" */
    private String convertToCodePoints(String text)
    {
    	StringBuffer codePointsText = new StringBuffer();
    	for(int i = 0; i < text.length(); ++i)
    	{
    		int unicode = text.codePointAt(i);
    		if(unicode > 126 || unicode < 32)
    		{
	    		codePointsText.append(Integer.toString(unicode));
    		}
    		else
    		{
    			codePointsText.append(text.charAt(i));
    		}
    	}
    	
    	return codePointsText.toString();
    }
    
    /** Get CRC32 of a file */
    public long getCRC32(File file) throws IOException
    {
    	final int BUFFER = 131072;
    	CRC32 crc = new CRC32();     
		byte[] crcBuffer = new byte[BUFFER];
		int crcRead;
		BufferedInputStream crcBis = new BufferedInputStream(new FileInputStream(file), BUFFER);
		crc.reset();
		while ((crcRead = crcBis.read(crcBuffer)) != -1) {
			crc.update(crcBuffer, 0, crcRead);
		}
		crcBis.close();
		return crc.getValue();	
    }
    
    /** Get CRC32 of a file */
    private long getCRC32(File file, Handler progressHandler, ProgressMessage hm) throws IOException, InterruptedException
    {    	
    	final int BUFFER = 131072;
    	CRC32 crc = new CRC32();     
		byte[] crcBuffer = new byte[BUFFER];
		int crcRead;
		BufferedInputStream crcBis = new BufferedInputStream(new FileInputStream(file), BUFFER);
		crc.reset();
		while ((crcRead = crcBis.read(crcBuffer)) != -1) {
			crc.update(crcBuffer, 0, crcRead);
			
			hm.setProgressAbs(hm.getProgressAbs() + (long)(CRC_TIMECOEF * crcRead));
			if(!hm.isRelSameAsLast()) progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
			checkThreadInterruption();
		}
		crcBis.close();
		return crc.getValue();	
    }
	
    private void checkThreadInterruption() throws InterruptedException
    {
    	if (Thread.interrupted()) throw new InterruptedException("Canceled by User.");
    }
    
    private void checkThreadInterruption(String wipeFilePath) throws InterruptedException
    {
    	if (Thread.interrupted()) throw new InterruptedException("Canceled by User.||" + wipeFilePath);
    }
    
	private static List<AlgorithmBean> algorithmsCodeBook()
	{
		List<AlgorithmBean> algorithms = new ArrayList<AlgorithmBean>();
		
		AlgorithmBean aes256 = new AlgorithmBean();
		aes256.code = ("AES");
		aes256.innerCode = 0;
		aes256.keySize = 256;
		aes256.blockSize = 128;
		aes256.provider = "BC_SSE";
		aes256.shortComment = "AES-256";
		aes256.comment = "AES (256 bit)";
		algorithms.add(aes256);
		
		AlgorithmBean rc6256 = new AlgorithmBean();
		rc6256.code = ("RC6");
		rc6256.innerCode = 1;
		rc6256.keySize = 256;
		rc6256.blockSize = 128;
		rc6256.provider = "BC_SSE";
		rc6256.shortComment = "RC6-256";
		rc6256.comment = "RC6 (256 bit)";
		algorithms.add(rc6256);
		
		AlgorithmBean serpent256 = new AlgorithmBean();
		serpent256.code = ("Serpent");
		serpent256.innerCode = 2;
		serpent256.keySize = 256;
		serpent256.blockSize = 128;
		serpent256.provider = "BC_SSE";
		serpent256.shortComment = "SERPENT-256";
		serpent256.comment = "Serpent (256 bit)";
		algorithms.add(serpent256);
		
		AlgorithmBean bf256 = new AlgorithmBean();
		bf256.code = ("BLOWFISH");
		bf256.innerCode = 3;
		bf256.keySize = 256;
		bf256.blockSize = 64;
		bf256.provider = "BC_SSE";
		bf256.shortComment = "BLOWFISH-256";
		bf256.comment = "Blowfish (256 bit)";
		algorithms.add(bf256);
		
		AlgorithmBean tf256 = new AlgorithmBean();
		tf256.code = ("TWOFISH");
		tf256.innerCode = 4;
		tf256.keySize = 256;
		tf256.blockSize = 128;
		tf256.provider = "BC_SSE";
		tf256.shortComment = "TWOFISH-256";
		tf256.comment = "Twofish (256 bit)";
		algorithms.add(tf256);
		
		AlgorithmBean gost256 = new AlgorithmBean();
		gost256.code = ("GOST28147");
		gost256.innerCode = 5;
		gost256.keySize = 256;
		gost256.blockSize = 64;
		gost256.provider = "BC_SSE";
		gost256.shortComment = "GOST-256";
		gost256.comment = "GOST28147 (256 bit)";
		algorithms.add(gost256);
		
		AlgorithmBean bf448 = new AlgorithmBean();
		bf448.code = ("BLOWFISH");
		bf448.innerCode = 6;
		bf448.keySize = 448;
		bf448.blockSize = 64;
		bf448.provider = "BC_SSE";
		bf448.shortComment = "BLOWFISH-448";
		bf448.comment = "Blowfish (448 bit)";
		algorithms.add(bf448);
		
		return algorithms;
	}
		
    public static class AlgorithmBean 
    {    
        private String code;
        private int innerCode;
        private int keySize;
        private int blockSize;
        private String shortComment = "";
        private String comment = "";
        private String provider;
        private boolean nativeCodeAvailable = false;
		
        public int getInnerCode() {
			return innerCode;
		}
        
        public int getBlockSize() {
			return blockSize;
		}
        
        public int getKeySize() {
			return keySize;
		}
        
        public boolean isNativeCodeAvailable() {
			return nativeCodeAvailable;
		}
		
        public String getComment() {
			return comment;
		}
        
        public String getShortComment() {
			return shortComment;
		} 
    }
}
