package com.tharindu.securespace;

/**
 * Class for storing constant values
 * @author Tharindu Wijewardane
 */

public class ConstVals {
	
	//preference keys
	public static final String PREF_KEY_PW = "pref_key_password";
	public static final String PREF_KEY_LOCATION_ENABLED = "pref_key_location_enabled";
	public static final String PREF_KEY_NFC_ENABLED = "pref_key_nfc_enabled";
	public static final String PREF_KEY_ENC_TAG = "pref_key_enc_tag";
	public static final String PREF_KEY_DEC_TAG = "pref_key_dec_tag";
	public static final String PREF_KEY_IS_ENCRYPTED = "pref_key_is_encrypted";

	//intent source
	public static final String REQUESTER_TYPE_KEY = "nfc_or_gps";
	public static final String REQUEST_FROM_NFC = "from_nfc";
	public static final String REQUEST_FROM_GPS = "from_gps";
	
	//NFC
	public static String TAG_TYPE_KEY = "tag_type";
	public static String TAG_TYPE_ENCRYPTOR = "tag_encryptor";
	public static String TAG_TYPE_DECRYPTOR = "tag_decryptor";
	
	//Location
	public static String COMMAND_TYPE_KEY = "command_type";
	public static String COMMAND_TYPE_ENCRYPT = "command_encrypt";
	public static String COMMAND_TYPE_DECRYPT = "command_decrypt";
	
	public static final String PREF_KEY_SELECTED_FILES_LIST = "pref_key_selected_files_list";
	
	//map
	public static final String PREF_KEY_LAT = "pref_key_lat";
	public static final String PREF_KEY_LON = "pref_key_lon";
	
}
