/*
 * Copyright (C) 2023 Alexander Gillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.alexandergillon.streamlet.node.services.impl;

import com.github.alexandergillon.streamlet.node.blockchain.Block;
import com.github.alexandergillon.streamlet.node.services.CryptographyService;
import com.github.alexandergillon.streamlet.node.util.SerializationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;

/** Implementation of a {@link CryptographyService}. */
@Service
public class CryptographyServiceImpl implements CryptographyService {

    @Value("${streamlet.participants}")
    private int numNodes;
    @Value("${streamlet.keystore.public.directory}")
    private String publicKeyDirectory;
    @Value("${streamlet.keystore.private.location}")
    private String privateKeyPath;
    @Value("${streamlet.keystore.private.alias}")
    private String privateKeyAlias;
    @Value("${streamlet.keystore.private.password}")
    private String privateKeyPassword;

    @Override
    public byte[] sign(Block block) {
        try {
            Signature signature = Signature.getInstance("SHA384withECDSA");
            signature.initSign(getPrivateKey());

            signature.update(block.toBytes());
            return signature.sign();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No SHA384withECDSA algorithm provider.", e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Private key for this node is invalid.", e);
        } catch (SignatureException e) {
            throw new IllegalStateException("Signature object has not been initialized correctly.", e);
        }

    }

    @Override
    public String signBase64(Block block) {
        return Base64.getEncoder().encodeToString(sign(block));
    }

    @Override
    public boolean validateProposal(Block block, byte[] signatureBytes) {
        int leader = leaderForEpoch(block.getEpoch());
        return validateVote(block, leader, signatureBytes);
    }

    @Override
    public boolean validateVote(Block block, int voter, byte[] signatureBytes) {
        try {
            PublicKey leaderPublicKey = getPublicKeyFor(voter);

            Signature signature = Signature.getInstance("SHA384withECDSA");
            signature.initVerify(leaderPublicKey);

            signature.update(block.toBytes());
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No SHA384withECDSA algorithm provider.", e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Public key for node " + voter + " is invalid.", e);
        } catch (SignatureException e) {
            throw new IllegalStateException("Signature object has not been initialized correctly.", e);
        }
    }

    @Override
    public int leaderForEpoch(int epoch) {
        try {
            byte[] bytes = SerializationUtils.intToFourBytesBigEndian(epoch);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(bytes);
            int pseudoRandomNumber = SerializationUtils.fourBytesToIntBigEndian(Arrays.copyOfRange(hash, 0, Integer.BYTES));
            int modulus = pseudoRandomNumber % numNodes;
            // modulus may be negative, if pseudoRandomNumber was negative - this step makes it positive in all cases
            return (modulus + numNodes) % numNodes;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No SHA-256 algorithm provider.", e);
        }
    }

    /** @return The private key for this node. */
    private PrivateKey getPrivateKey() {
        try {
            KeyStore keyStore = getPrivateKeyStore();
            return (PrivateKey) keyStore.getKey(privateKeyAlias, privateKeyPassword.toCharArray());
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Keystore has not been initialized correctly.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /** @return The keystore for the private key for this node. */
    private KeyStore getPrivateKeyStore() {
        try {
            FileInputStream keystoreFile = new FileInputStream(privateKeyPath);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(keystoreFile, privateKeyPassword.toCharArray());
            return keyStore;
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Private key for this node not found.", e);
        } catch (KeyStoreException e) {
            throw new IllegalStateException("No PKCS12 keystore provider.", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (CertificateException e) {
            throw new IllegalStateException("Private key certificate for this node not found.", e);
        }
    }

    /**
     * Gets the public key for a node.
     *
     * @param nodeId The ID of the node.
     * @return The public key of that node.
     */
    private PublicKey getPublicKeyFor(int nodeId) {
        try {
            CertificateFactory certificateFactory = getX509CertificateFactory();
            FileInputStream certificateFile = new FileInputStream(buildCertificatePath(nodeId));
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certificateFile);
            return certificate.getPublicKey();
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Public key for node " + nodeId + " not found.", e);
        }
    }

    /** @return An X.509 {@link CertificateFactory}. */
    private CertificateFactory getX509CertificateFactory() {
        try {
            return CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new IllegalStateException("No X.509 certificate provider.", e);
        }
    }

    /**
     * Builds the filepath of the public key certificate of a node.
     *
     * @param nodeId The ID of the node.
     * @return The path to that node's public key certificate.
     */
    private String buildCertificatePath(int nodeId) {
        Path directory = Path.of(publicKeyDirectory);
        return directory.resolve("node" + nodeId + "_public_key.cer").toString();
    }

}
