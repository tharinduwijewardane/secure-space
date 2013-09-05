package com.paranoiaworks.unicus.android.sse.utils;

import sse.org.bouncycastle.crypto.BlockCipher;
import sse.org.bouncycastle.crypto.BufferedBlockCipher;
import sse.org.bouncycastle.crypto.CipherParameters;
import sse.org.bouncycastle.crypto.engines.AESFastEngine;
import sse.org.bouncycastle.crypto.engines.BlowfishEngine;
import sse.org.bouncycastle.crypto.engines.GOST28147Engine;
import sse.org.bouncycastle.crypto.engines.RC6Engine;
import sse.org.bouncycastle.crypto.engines.SerpentEngine;
import sse.org.bouncycastle.crypto.engines.TwofishEngine;
import sse.org.bouncycastle.crypto.modes.CBCBlockCipher;
import sse.org.bouncycastle.crypto.paddings.BlockCipherPadding;
import sse.org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import sse.org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import sse.org.bouncycastle.crypto.params.KeyParameter;
import sse.org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Cipher Provider with Bouncy Castle lightweight API
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.0
 */
public class CipherProvider {
	
	public static final int ALG_AES = 0;
	public static final int ALG_RC6 = 1;
	public static final int ALG_SERPENT = 2;
	public static final int ALG_BLOWFISH = 3;
	public static final int ALG_TWOFISH = 4;
	public static final int ALG_GOST28147 = 5;
	
	public static BufferedBlockCipher getBufferedBlockCipher(boolean forEncryption, byte[] iv, byte[] key, int algorithmCode)
	{
		return getBufferedBlockCipher(forEncryption, iv, key, algorithmCode, true);
	}
	
	public static BufferedBlockCipher getBufferedBlockCipher(boolean forEncryption, byte[] iv, byte[] key, int algorithmCode, boolean withPadding)
	{
		BufferedBlockCipher cipher = null;
  	    KeyParameter keyParam = new KeyParameter(key);
  	    CipherParameters params = new ParametersWithIV(keyParam, iv);
  	    BlockCipherPadding padding = new ISO10126d2Padding();	
  	    cipher = withPadding ? new PaddedBufferedBlockCipher(getBaseCBCCipher(algorithmCode), padding) : new BufferedBlockCipher(getBaseCBCCipher(algorithmCode));
		cipher.init(forEncryption, params);
		
		return cipher;
	}
	
	private static BlockCipher getBaseCBCCipher(int algorithmCode)
	{
		BlockCipher baseCipher = null;
		switch (algorithmCode)
        {        	
        	case 0: 
        	{
        		baseCipher = new CBCBlockCipher(new AESFastEngine());
            	break;
        	}
        	case 1: 
        	{
        		baseCipher = new CBCBlockCipher(new RC6Engine());
            	break;
        	}
        	case 2: 
        	{
        		baseCipher = new CBCBlockCipher(new SerpentEngine());
            	break;
        	}
        	case 3: 
        	{
        		baseCipher = new CBCBlockCipher(new BlowfishEngine());
            	break;
        	}
        	case 4: 
        	{
        		baseCipher = new CBCBlockCipher(new TwofishEngine());
            	break;
        	}
        	case 5: 
        	{
        		baseCipher = new CBCBlockCipher(new GOST28147Engine());
            	break;
        	}
        	case 6:
        	{
        		baseCipher = new CBCBlockCipher(new BlowfishEngine());
            	break;
        	}
            
        	default: 
            	break;
        }
		
		return baseCipher;
	}
}
