package pt.tecnico.sec.bftb.client;

import pt.tecnico.sec.bftb.client.exceptions.KeyPairGenerationFailedException;
import pt.tecnico.sec.bftb.client.exceptions.KeyPairLoadingFailedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Resources {

	public static final String PRIVKEY_FILENAME = "private.key";
	public static final String PUBKEY_FILENAME = "public.key";
	private static final String KEYPAIR_PATH = "keypairs";
	private static final String SERVER_CERT_PATH = "servercert";
	private static final String SERVER_CERT_FILENAME = "cert.pem";

	private Resources() { /* empty */ }

	public static PublicKey getPublicKeyByUserId(String userId)
			throws KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		// Check if userId exists, if not, generate new keypair for it
		if (isKeyPairNonexistent(userId)) generateKeyPair(userId);

		try {
			String pathString = Path.of(KEYPAIR_PATH, userId, PUBKEY_FILENAME).toString();
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
			assert keyStream != null;
			byte[] keyBytes = keyStream.readAllBytes();
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			return KeyFactory.getInstance("RSA").generatePublic(spec);
		}
		catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new KeyPairLoadingFailedException(e);
		}
	}

	public static PrivateKey getPrivateKeyByUserId(String userId)
			throws KeyPairGenerationFailedException, KeyPairLoadingFailedException {
		// Check if userId exists, if not, generate new keypair for it
		if (isKeyPairNonexistent(userId)) generateKeyPair(userId);

		try {
			String pathString = Path.of(KEYPAIR_PATH, userId, PRIVKEY_FILENAME).toString();
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
			assert keyStream != null;
			byte[] keyBytes = keyStream.readAllBytes();
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			return KeyFactory.getInstance("RSA").generatePrivate(spec);
		}
		catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new KeyPairLoadingFailedException(e);
		}
	}

	private static void generateKeyPair(String userId) throws KeyPairGenerationFailedException {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair keyPair = keyGen.generateKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();

			URL resource = Resources.class.getClassLoader().getResource(".");
			assert resource != null;
			Path resourcesPath = Paths.get(resource.toURI());
			Path keypairPath = resourcesPath.resolve(KEYPAIR_PATH).resolve(userId);
			Files.createDirectories(keypairPath);

			Files.write(keypairPath.resolve(PUBKEY_FILENAME), publicKey.getEncoded());
			Files.write(keypairPath.resolve(PRIVKEY_FILENAME), privateKey.getEncoded());
		}
		catch (NullPointerException | NoSuchAlgorithmException | IOException | URISyntaxException e) {
			throw new KeyPairGenerationFailedException(e);
		}
	}

	private static boolean isKeyPairNonexistent(String userId) {
		return Resources.class.getClassLoader().getResource(Path.of(KEYPAIR_PATH, userId).toString()) == null ||
				Resources.class.getClassLoader().getResource(Path.of(KEYPAIR_PATH, userId, PUBKEY_FILENAME).toString()) == null ||
				Resources.class.getClassLoader().getResource(Path.of(KEYPAIR_PATH, userId, PRIVKEY_FILENAME).toString()) == null;
	}

	public static PublicKey getServerPublicKey() throws CertificateException {
		InputStream certStream = Resources.class.getClassLoader().getResourceAsStream(Path.of(SERVER_CERT_PATH, SERVER_CERT_FILENAME).toString());
		CertificateFactory f = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) f.generateCertificate(certStream);
		return certificate.getPublicKey();
	}
}
