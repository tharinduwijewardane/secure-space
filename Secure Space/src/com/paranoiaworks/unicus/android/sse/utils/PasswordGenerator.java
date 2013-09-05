package com.paranoiaworks.unicus.android.sse.utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sse.org.bouncycastle.crypto.prng.ThreadedSeedGenerator;

/**
 * Password Generator
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.0
 */
public class PasswordGenerator {

	private String charSet = "";
	private int charSetSize;
	private int[] substituteCache = new int[3];
	private int substituteCounter;
	
	private String lowerAlphaChars;
	private String upperAlphaChars;
	private String numberChars;
	private String specCharChars;
	private List<String> charSetList = new ArrayList<String>();
	
	
	public PasswordGenerator(boolean lowerAlpha, boolean upperAlpha, boolean number, boolean specChar, boolean removeMisspelling)
	{	
		if(removeMisspelling)
		{
			// 0O'`1l|I
			this.lowerAlphaChars = "abcdefghijkmnopqrstuvwxyz";
			this.upperAlphaChars = "ABCDEFGHJKLMNPQRSTUVWXYZ";
			this.numberChars = "23456789";
			this.specCharChars = "!\"#$%&()*+,-./:;<=>?@[\\]^_{}~";
		}
		else
		{
			this.lowerAlphaChars = "abcdefghijklmnopqrstuvwxyz";
			this.upperAlphaChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
			this.numberChars = "0123456789";
			this.specCharChars = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
		}
		
		if(lowerAlpha) {
			charSet += lowerAlphaChars;
			charSetList.add(lowerAlphaChars);
		}
		if(upperAlpha) {
			charSet += upperAlphaChars;
			charSetList.add(upperAlphaChars);
		}
		if(number) {
			charSet += numberChars;
			charSetList.add(numberChars);
		}
		if(specChar) {
			charSet += specCharChars;
			charSetList.add(specCharChars);
		}
		
		charSetSize = charSet.length();
	}
	
	public String getNewPassword(int length)
	{
		if(length < 4) length = 4;
		if(length > 64) length = 64;
		substituteCounter = 0;
		StringBuffer password = new StringBuffer(length);
		byte[] randomBytesBuffer = getRandomBA();
		
		for(int i = 0; i < length * 2; i += 2)
		{
			int temp = (randomBytesBuffer[i] + 128 ) + (randomBytesBuffer[i + 1] + 128);
			password.append(charSet.charAt(temp % charSetSize));
		}
		
		String output = balancePassword(password.toString());
		
		return output;
	}
	
	/** Get Random bytes using ThreadedSeedGenerator */
	private byte[] getRandomBA()
	{	
		ThreadedSeedGenerator tsg = new ThreadedSeedGenerator();
		byte[] tsgOutput = tsg.generateSeed(64, true);
		byte[] timeOutput = String.valueOf(System.currentTimeMillis()).getBytes();
		byte[] seed = Helpers.concat(tsgOutput, timeOutput);
		byte[] output = null;
		substituteCache[0] = (tsgOutput[63] + 128) + (tsgOutput[62] + 128);
		substituteCache[1] = (tsgOutput[61] + 128) + (tsgOutput[60] + 128);
		substituteCache[2] = (tsgOutput[59] + 128) + (tsgOutput[58] + 128);
		
		try {
			SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
			rand.setSeed(seed);
			byte[]randomNum = new byte[128];
			rand.nextBytes(randomNum);
			MessageDigest sha = MessageDigest.getInstance("SHA-512");
			output = sha.digest(randomNum);
			byte[] s1 = Helpers.getSubarray(output, 0, 32);
			byte[] s2 = Helpers.getSubarray(output, 32, 32);
			byte[] o1 = sha.digest(s1);
			byte[] o2 = sha.digest(s2);
			output = Helpers.concat(o1, o2);			
			output = Helpers.xorit(output, tsg.generateSeed(128, true));
		} catch (Exception e1) {
			e1.printStackTrace();
		}  
		return output;
	}
	
	private String balancePassword(String password)
	{
		int zeroCounter = 4;
		TreeMap<Integer, Integer> sortMap = new TreeMap<Integer, Integer>();
		
		while(zeroCounter > 0)
		{	
			if(sortMap.size() > 0)
			{
				String max = charSetList.get(sortMap.get(sortMap.lastKey()));
				String min = charSetList.get(sortMap.get(sortMap.firstKey()));
				String replacement = Character.toString(min.charAt(substituteCache[substituteCounter] % min.length()));
				if(replacement.equals("$") || replacement.equals("\\")) replacement = Matcher.quoteReplacement(replacement);
				password = password.replaceFirst("[" + Pattern.quote(max) + "]", replacement);
				++substituteCounter;
			}
			
			sortMap.clear();
			zeroCounter = 0;
			for(int i = 0; i < charSetList.size(); ++i)
			{
				int count = Helpers.regexGetCountOf(password, "[" + Pattern.quote(charSetList.get(i)) + "]");
				if(count == 0) ++zeroCounter;
				sortMap.put(count, i);		
			}			
		}
		return password;
	}
}
