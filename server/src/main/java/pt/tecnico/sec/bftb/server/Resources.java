package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.PrivateKeyLoadingFailedException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;

public class Resources {

	private static final String CRYPTO_PATH = "crypto";
	private static final String KEYSTORE_FILENAME = "keystore.jks";
	private static final String KEYSTORE_PWD = "sec2122";

	private Resources() { /* empty */ }

	public static PrivateKey getPrivateKey() throws PrivateKeyLoadingFailedException {
		try {
			String pathString = Path.of(CRYPTO_PATH, KEYSTORE_FILENAME).toString();
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
			assert keyStream != null;
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(keyStream, KEYSTORE_PWD.toCharArray());
			return (PrivateKey) ks.getKey("1", KEYSTORE_PWD.toCharArray());
		}
		catch (KeyStoreException | UnrecoverableKeyException | CertificateException | IOException | NoSuchAlgorithmException e) {
			throw new PrivateKeyLoadingFailedException(e);
		}
	}
}
