package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.*;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;

public class Resources {

	private static final String CRYPTO_PATH = "crypto";
	private static final String KEYSTORE_FILENAME = "keystore.jks";
	private static final String KEYSTORE_PWD = "sec2122";

	private static final String DATABASE_PATH = "database";
	private static final String DATABASE_FILENAME = "database_%d.sqlite";

	private Resources() { /* empty */ }

	public static void init() throws DirectoryCreationFailedException {
		createResourceDirectory(DATABASE_PATH);
	}

	public static String getAbsoluteDatabasePath(int n) throws URISyntaxException {
		return Path.of(getAbsolutePathOfResource(DATABASE_PATH), String.format(DATABASE_FILENAME, n)).toString();
	}

	private static String getAbsolutePathOfResource(String path) throws URISyntaxException {
		String pathString = Path.of(path).toString();
		URL pathURL = Resources.class.getClassLoader().getResource(pathString);
		assert pathURL != null;
		return Paths.get(pathURL.toURI()).toString();
	}

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

	private static void createResourceDirectory(String dirname) throws DirectoryCreationFailedException {
		try {
			URL resource = Resources.class.getClassLoader().getResource(".");
			assert resource != null;
			Path resourcesPath = Paths.get(resource.toURI());
			Path dirPath = resourcesPath.resolve(dirname);
			Files.createDirectories(dirPath);
		}
		catch (URISyntaxException | IOException e) {
			throw new DirectoryCreationFailedException(e);
		}
	}
}
