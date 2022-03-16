package pt.tecnico.sec.bftb.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

public class Resources {

	private static final String KEYPAIR_PATH = "keypairs/";

	private Resources() { /* empty */ }

	public static PublicKey getPublicKeyByUserId(String userId) {
		// Check if userId exists, if not, generate new keypair for it
		if (!checkIfKeyPairExists(userId)) generateKeyPair(userId);

		try {
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(KEYPAIR_PATH + userId + "/public.key");
			assert keyStream != null;
			byte[] keyBytes = keyStream.readAllBytes();
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			return KeyFactory.getInstance("RSA").generatePublic(spec);
		}
		catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static PrivateKey getPrivateKeyByUserId(String userId) {
		// Check if userId exists, if not, generate new keypair for it
		if (!checkIfKeyPairExists(userId)) generateKeyPair(userId);

		try {
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(KEYPAIR_PATH + userId + "/private.key");
			assert keyStream != null;
			byte[] keyBytes = keyStream.readAllBytes();
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			return KeyFactory.getInstance("RSA").generatePrivate(spec);
		}
		catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static void generateKeyPair(String userId) {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair keyPair = keyGen.generateKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();

			String keyPairsPathString = Objects.requireNonNull(Resources.class.getClassLoader().getResource(KEYPAIR_PATH)).getPath();
			Path keypairPath = Path.of( keyPairsPathString + "/" + userId);
			Files.createDirectories(keypairPath);

			Files.write(keypairPath.resolve("public.key"), publicKey.getEncoded());
			Files.write(keypairPath.resolve("private.key"), privateKey.getEncoded());
		}
		catch (NullPointerException | NoSuchAlgorithmException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean checkIfKeyPairExists(String userId) {
		return Resources.class.getClassLoader().getResource(KEYPAIR_PATH + userId) != null &&
				Resources.class.getClassLoader().getResource(KEYPAIR_PATH + userId + "/public.key") != null &&
				Resources.class.getClassLoader().getResource(KEYPAIR_PATH + userId + "/private.key") != null;
	}
}
