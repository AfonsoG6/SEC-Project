package pt.tecnico.sec.bftb.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Resources {

	private static final String KEYPAIR_PATH = "keypairs";
	private static final String PUB_KEY_FILENAME = "pubkey.pem";
	private static final String PRIV_KEY_FILENAME = "privkey_pkcs8.pem";

	private Resources() { /* empty */ }

	public static PublicKey getPublicKey() {
		try {
			String pathString = Path.of(KEYPAIR_PATH, PUB_KEY_FILENAME).toString();
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
			assert keyStream != null;
			byte[] keyBytes = keyStream.readAllBytes();
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			return KeyFactory.getInstance("RSA").generatePublic(spec);
		}
		catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static PrivateKey getPrivateKey() {
		try {
			String pathString = Path.of(KEYPAIR_PATH, PRIV_KEY_FILENAME).toString();
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
			assert keyStream != null;
			byte[] keyBytes = keyStream.readAllBytes();
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			return KeyFactory.getInstance("RSA").generatePrivate(spec);
		}
		catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
