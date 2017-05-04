/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.apksig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNoException;

import com.android.apksig.ApkVerifier.Issue;
import com.android.apksig.ApkVerifier.IssueWithParams;
import com.android.apksig.apk.ApkFormatException;
import com.android.apksig.internal.test.HexEncoding;
import com.android.apksig.internal.util.AndroidSdkVersion;
import com.android.apksig.util.DataSources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ApkVerifierTest {

    private static final String[] DSA_KEY_NAMES = {"1024", "2048", "3072"};
    private static final String[] DSA_KEY_NAMES_1024_AND_SMALLER = {"1024"};
    private static final String[] DSA_KEY_NAMES_2048_AND_LARGER = {"2048", "3072"};
    private static final String[] EC_KEY_NAMES = {"p256", "p384", "p521"};
    private static final String[] RSA_KEY_NAMES = {"1024", "2048", "3072", "4096", "8192", "16384"};
    private static final String[] RSA_KEY_NAMES_2048_AND_LARGER =
            {"2048", "3072", "4096", "8192", "16384"};

    @Test
    public void testOriginalAccepted() throws Exception {
        // APK signed with v1 and v2 schemes. Obtained by building
        // cts/hostsidetests/appsecurity/test-apps/tinyapp.
        // This APK is used as a basis for many of the other tests here. Hence, we check that this
        // APK verifies.
        assertVerified(verify("original.apk"));
    }

    @Test
    public void testV1OneSignerMD5withRSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-md5-1.2.840.113549.1.1.1-%s.apk", RSA_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-md5-1.2.840.113549.1.1.4-%s.apk", RSA_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA1withRSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha1-1.2.840.113549.1.1.1-%s.apk", RSA_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha1-1.2.840.113549.1.1.5-%s.apk", RSA_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA224withRSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha224-1.2.840.113549.1.1.1-%s.apk", RSA_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha224-1.2.840.113549.1.1.14-%s.apk", RSA_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA256withRSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha256-1.2.840.113549.1.1.1-%s.apk", RSA_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha256-1.2.840.113549.1.1.11-%s.apk", RSA_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA384withRSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha384-1.2.840.113549.1.1.1-%s.apk", RSA_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha384-1.2.840.113549.1.1.12-%s.apk", RSA_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA512withRSAVerifies() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha512-1.2.840.113549.1.1.1-%s.apk", RSA_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-rsa-pkcs1-sha512-1.2.840.113549.1.1.13-%s.apk", RSA_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA1withECDSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha1-1.2.840.10045.2.1-%s.apk", EC_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha1-1.2.840.10045.4.1-%s.apk", EC_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA224withECDSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha224-1.2.840.10045.2.1-%s.apk", EC_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha224-1.2.840.10045.4.3.1-%s.apk", EC_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA256withECDSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha256-1.2.840.10045.2.1-%s.apk", EC_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha256-1.2.840.10045.4.3.2-%s.apk", EC_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA384withECDSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha384-1.2.840.10045.2.1-%s.apk", EC_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha384-1.2.840.10045.4.3.3-%s.apk", EC_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA512withECDSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha512-1.2.840.10045.2.1-%s.apk", EC_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-ecdsa-sha512-1.2.840.10045.4.3.4-%s.apk", EC_KEY_NAMES);
    }

    @Test
    public void testV1OneSignerSHA1withDSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        // NOTE: This test is split into two because JCA Providers shipping with OpenJDK refuse to
        // verify DSA signatures with keys too long for the SHA-1 digest.
        assertVerifiedForEach(
                "v1-only-with-dsa-sha1-1.2.840.10040.4.1-%s.apk", DSA_KEY_NAMES_1024_AND_SMALLER);
        assertVerifiedForEach(
                "v1-only-with-dsa-sha1-1.2.840.10040.4.3-%s.apk", DSA_KEY_NAMES_1024_AND_SMALLER);
    }

    @Test
    public void testV1OneSignerSHA1withDSAAcceptedWithKeysTooLongForDigest() throws Exception {
        // APK signed with v1 scheme only, one signer

        // OpenJDK's default implementation of Signature.SHA1withDSA refuses to verify signatures
        // created with keys too long for the digest used. Android Package Manager does not reject
        // such signatures. We thus skip this test if Signature.SHA1withDSA exhibits this issue.
        PublicKey publicKey = getCertificateFromResources("dsa-2048.x509.pem").getPublicKey();
        Signature s = Signature.getInstance("SHA1withDSA");
        try {
            s.initVerify(publicKey);
        } catch (InvalidKeyException e) {
            assumeNoException(e);
        }

        assertVerifiedForEach(
                "v1-only-with-dsa-sha1-1.2.840.10040.4.1-%s.apk", DSA_KEY_NAMES_2048_AND_LARGER);
        assertVerifiedForEach(
                "v1-only-with-dsa-sha1-1.2.840.10040.4.3-%s.apk", DSA_KEY_NAMES_2048_AND_LARGER);
    }

    @Test
    public void testV1OneSignerSHA224withDSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        // NOTE: This test is split into two because JCA Providers shipping with OpenJDK refuse to
        // verify DSA signatures with keys too long for the SHA-224 digest.
        assertVerifiedForEach(
                "v1-only-with-dsa-sha224-1.2.840.10040.4.1-%s.apk", DSA_KEY_NAMES_1024_AND_SMALLER);
        assertVerifiedForEach(
                "v1-only-with-dsa-sha224-2.16.840.1.101.3.4.3.1-%s.apk",
                DSA_KEY_NAMES_1024_AND_SMALLER);
    }

    @Test
    public void testV1OneSignerSHA224withDSAAcceptedWithKeysTooLongForDigest() throws Exception {
        // APK signed with v1 scheme only, one signer

        // OpenJDK's default implementation of Signature.SHA224withDSA refuses to verify signatures
        // created with keys too long for the digest used. Android Package Manager does not reject
        // such signatures. We thus skip this test if Signature.SHA224withDSA exhibits this issue.
        PublicKey publicKey = getCertificateFromResources("dsa-2048.x509.pem").getPublicKey();
        Signature s = Signature.getInstance("SHA224withDSA");
        try {
            s.initVerify(publicKey);
        } catch (InvalidKeyException e) {
            assumeNoException(e);
        }
        assertVerifiedForEach(
                "v1-only-with-dsa-sha224-1.2.840.10040.4.1-%s.apk", DSA_KEY_NAMES_2048_AND_LARGER);
        assertVerifiedForEach(
                "v1-only-with-dsa-sha224-2.16.840.1.101.3.4.3.1-%s.apk",
                DSA_KEY_NAMES_2048_AND_LARGER);
    }

    @Test
    public void testV1OneSignerSHA256withDSAAccepted() throws Exception {
        // APK signed with v1 scheme only, one signer
        assertVerifiedForEach(
                "v1-only-with-dsa-sha256-1.2.840.10040.4.1-%s.apk", DSA_KEY_NAMES);
        assertVerifiedForEach(
                "v1-only-with-dsa-sha256-2.16.840.1.101.3.4.3.2-%s.apk", DSA_KEY_NAMES);
    }

    @Test
    public void testV2StrippedRejected() throws Exception {
        // APK signed with v1 and v2 schemes, but v2 signature was stripped from the file (by using
        // zipalign).
        // This should fail because the v1 signature indicates that the APK was supposed to be
        // signed with v2 scheme as well, making the platform's anti-stripping protections reject
        // the APK.
        assertVerificationFailure(
                "v2-stripped.apk", Issue.JAR_SIG_MISSING_APK_SIG_REFERENCED);

        // Similar to above, but the X-Android-APK-Signed anti-stripping header in v1 signature
        // lists unknown signature schemes in addition to APK Signature Scheme v2. Unknown schemes
        // should be ignored.
        assertVerificationFailure(
                "v2-stripped-with-ignorable-signing-schemes.apk",
                Issue.JAR_SIG_MISSING_APK_SIG_REFERENCED);
    }

    @Test
    public void testV2OneSignerOneSignatureAccepted() throws Exception {
        // APK signed with v2 scheme only, one signer, one signature
        assertVerifiedForEachForMinSdkVersion(
                "v2-only-with-dsa-sha256-%s.apk", DSA_KEY_NAMES, AndroidSdkVersion.N);
        assertVerifiedForEachForMinSdkVersion(
                "v2-only-with-ecdsa-sha256-%s.apk", EC_KEY_NAMES, AndroidSdkVersion.N);
        assertVerifiedForEachForMinSdkVersion(
                "v2-only-with-rsa-pkcs1-sha256-%s.apk", RSA_KEY_NAMES, AndroidSdkVersion.N);
        // RSA-PSS signatures tested in a separate test below

        // DSA with SHA-512 is not supported by Android platform and thus APK Signature Scheme v2
        // does not support that either
        // assertInstallSucceedsForEach("v2-only-with-dsa-sha512-%s.apk", DSA_KEY_NAMES);
        assertVerifiedForEachForMinSdkVersion(
                "v2-only-with-ecdsa-sha512-%s.apk", EC_KEY_NAMES, AndroidSdkVersion.N);
        assertVerifiedForEachForMinSdkVersion(
                "v2-only-with-rsa-pkcs1-sha512-%s.apk", RSA_KEY_NAMES, AndroidSdkVersion.N);
    }

    @Test
    public void testV2OneSignerOneRsaPssSignatureAccepted() throws Exception {
        assumeThatRsaPssAvailable();
        // APK signed with v2 scheme only, one signer, one signature
        assertVerifiedForEachForMinSdkVersion(
                "v2-only-with-rsa-pss-sha256-%s.apk", RSA_KEY_NAMES, AndroidSdkVersion.N);
        assertVerifiedForEachForMinSdkVersion(
                "v2-only-with-rsa-pss-sha512-%s.apk",
                RSA_KEY_NAMES_2048_AND_LARGER, // 1024-bit key is too short for PSS with SHA-512
                AndroidSdkVersion.N);
    }

    @Test
    public void testV2SignatureDoesNotMatchSignedDataRejected() throws Exception {
        // APK signed with v2 scheme only, but the signature over signed-data does not verify

        // Bitflip in certificate field inside signed-data. Based on
        // v2-only-with-dsa-sha256-1024.apk.
        assertVerificationFailure(
                "v2-only-with-dsa-sha256-1024-sig-does-not-verify.apk",
                Issue.V2_SIG_DID_NOT_VERIFY);

        // Signature claims to be RSA PKCS#1 v1.5 with SHA-256, but is actually using SHA-512.
        // Based on v2-only-with-rsa-pkcs1-sha256-2048.apk.
        assertVerificationFailure(
                "v2-only-with-rsa-pkcs1-sha256-2048-sig-does-not-verify.apk",
                Issue.V2_SIG_VERIFY_EXCEPTION);

        // Bitflip in the ECDSA signature. Based on v2-only-with-ecdsa-sha256-p256.apk.
        assertVerificationFailure(
                "v2-only-with-ecdsa-sha256-p256-sig-does-not-verify.apk",
                Issue.V2_SIG_DID_NOT_VERIFY);
    }

    @Test
    public void testV2RsaPssSignatureDoesNotMatchSignedDataRejected() throws Exception {
        assumeThatRsaPssAvailable();

        // APK signed with v2 scheme only, but the signature over signed-data does not verify.

        // Signature claims to be RSA PSS with SHA-256 and 32 bytes of salt, but is actually using 0
        // bytes of salt. Based on v2-only-with-rsa-pkcs1-sha256-2048.apk. Obtained by modifying APK
        // signer to use the wrong amount of salt.
        assertVerificationFailure(
                "v2-only-with-rsa-pss-sha256-2048-sig-does-not-verify.apk",
                Issue.V2_SIG_DID_NOT_VERIFY);
    }

    @Test
    public void testV2ContentDigestMismatchRejected() throws Exception {
        // APK signed with v2 scheme only, but the digest of contents does not match the digest
        // stored in signed-data
        ApkVerifier.Issue error = Issue.V2_SIG_APK_DIGEST_DID_NOT_VERIFY;

        // Based on v2-only-with-rsa-pkcs1-sha512-4096.apk. Obtained by modifying APK signer to
        // flip the leftmost bit in content digest before signing signed-data.
        assertVerificationFailure(
                "v2-only-with-rsa-pkcs1-sha512-4096-digest-mismatch.apk", error);

        // Based on v2-only-with-ecdsa-sha256-p256.apk. Obtained by modifying APK signer to flip the
        // leftmost bit in content digest before signing signed-data.
        assertVerificationFailure(
                "v2-only-with-ecdsa-sha256-p256-digest-mismatch.apk", error);
    }

    @Test
    public void testNoApkSignatureSchemeBlockRejected() throws Exception {
        // APK signed with v2 scheme only, but the rules for verifying APK Signature Scheme v2
        // signatures say that this APK must not be verified using APK Signature Scheme v2.

        // Obtained from v2-only-with-rsa-pkcs1-sha512-4096.apk by flipping a bit in the magic
        // field in the footer of APK Signing Block. This makes the APK Signing Block disappear.
        assertVerificationFailure(
                "v2-only-wrong-apk-sig-block-magic.apk",
                Issue.JAR_SIG_NO_MANIFEST);

        // Obtained by modifying APK signer to insert "GARBAGE" between ZIP Central Directory and
        // End of Central Directory. The APK is otherwise fine and is signed with APK Signature
        // Scheme v2. Based on v2-only-with-rsa-pkcs1-sha256.apk.
        assertVerificationFailure(
                "v2-only-garbage-between-cd-and-eocd.apk",
                Issue.JAR_SIG_NO_MANIFEST);

        // Obtained by modifying the size in APK Signature Block header. Based on
        // v2-only-with-ecdsa-sha512-p521.apk.
        assertVerificationFailure(
                "v2-only-apk-sig-block-size-mismatch.apk",
                Issue.JAR_SIG_NO_MANIFEST);

        // Obtained by modifying the ID under which APK Signature Scheme v2 Block is stored in
        // APK Signing Block and by modifying the APK signer to not insert anti-stripping
        // protections into JAR Signature. The APK should appear as having no APK Signature Scheme
        // v2 Block and should thus successfully verify using JAR Signature Scheme.
        assertVerified(verify("v1-with-apk-sig-block-but-without-apk-sig-scheme-v2-block.apk"));
    }

    @Test(expected = ApkFormatException.class)
    public void testTruncatedZipCentralDirectoryRejected() throws Exception {
        // Obtained by modifying APK signer to truncate the ZIP Central Directory by one byte. The
        // APK is otherwise fine and is signed with APK Signature Scheme v2. Based on
        // v2-only-with-rsa-pkcs1-sha256.apk
        verify("v2-only-truncated-cd.apk");
    }

    @Test
    public void testV2UnknownPairIgnoredInApkSigningBlock() throws Exception {
        // Obtained by modifying APK signer to emit an unknown ID-value pair into APK Signing Block
        // before the ID-value pair containing the APK Signature Scheme v2 Block. The unknown
        // ID-value should be ignored.
        assertVerified(
                verifyForMinSdkVersion(
                        "v2-only-unknown-pair-in-apk-sig-block.apk", AndroidSdkVersion.N));
    }

    @Test
    public void testV2UnknownSignatureAlgorithmsIgnored() throws Exception {
        // APK is signed with a known signature algorithm and with a couple of unknown ones.
        // Obtained by modifying APK signer to use "unknown" signature algorithms in addition to
        // known ones.
        assertVerified(
                verifyForMinSdkVersion(
                        "v2-only-with-ignorable-unsupported-sig-algs.apk", AndroidSdkVersion.N));
    }

    @Test
    public void testV2MismatchBetweenSignaturesAndDigestsBlockRejected() throws Exception {
        // APK is signed with a single signature algorithm, but the digests block claims that it is
        // signed with two different signature algorithms. Obtained by modifying APK Signer to
        // emit an additional digest record with signature algorithm 0x12345678.
        assertVerificationFailure(
                "v2-only-signatures-and-digests-block-mismatch.apk",
                Issue.V2_SIG_SIG_ALG_MISMATCH_BETWEEN_SIGNATURES_AND_DIGESTS_RECORDS);
    }

    @Test
    public void testV2MismatchBetweenPublicKeyAndCertificateRejected() throws Exception {
        // APK is signed with v2 only. The public key field does not match the public key in the
        // leaf certificate. Obtained by modifying APK signer to write out a modified leaf
        // certificate where the RSA modulus has a bitflip.
        assertVerificationFailure(
                "v2-only-cert-and-public-key-mismatch.apk",
                Issue.V2_SIG_PUBLIC_KEY_MISMATCH_BETWEEN_CERTIFICATE_AND_SIGNATURES_RECORD);
    }

    @Test
    public void testV2SignerBlockWithNoCertificatesRejected() throws Exception {
        // APK is signed with v2 only. There are no certificates listed in the signer block.
        // Obtained by modifying APK signer to output no certificates.
        assertVerificationFailure(
                "v2-only-no-certs-in-sig.apk", Issue.V2_SIG_NO_CERTIFICATES);
    }

    @Test
    public void testTwoSignersAccepted() throws Exception {
        // APK signed by two different signers
        assertVerified(verify("two-signers.apk"));
        assertVerified(verify("v1-only-two-signers.apk"));
        assertVerified(verifyForMinSdkVersion("v2-only-two-signers.apk", AndroidSdkVersion.N));
    }

    @Test
    public void testV2TwoSignersRejectedWhenOneBroken() throws Exception {
        // Bitflip in the ECDSA signature of second signer. Based on two-signers.apk.
        // This asserts that breakage in any signer leads to rejection of the APK.
        assertVerificationFailure(
                "two-signers-second-signer-v2-broken.apk", Issue.V2_SIG_DID_NOT_VERIFY);
    }

    @Test
    public void testV2TwoSignersRejectedWhenOneWithoutSignatures() throws Exception {
        // APK v2-signed by two different signers. However, there are no signatures for the second
        // signer.
        assertVerificationFailure(
                "v2-only-two-signers-second-signer-no-sig.apk", Issue.V2_SIG_NO_SIGNATURES);
    }

    @Test
    public void testV2TwoSignersRejectedWhenOneWithoutSupportedSignatures() throws Exception {
        // APK v2-signed by two different signers. However, there are no supported signatures for
        // the second signer.
        assertVerificationFailure(
                "v2-only-two-signers-second-signer-no-supported-sig.apk",
                Issue.V2_SIG_NO_SUPPORTED_SIGNATURES);
    }

    @Test
    public void testCorrectCertUsedFromPkcs7SignedDataCertsSet() throws Exception {
        // Obtained by prepending the rsa-1024 certificate to the PKCS#7 SignedData certificates set
        // of v1-only-with-rsa-pkcs1-sha1-1.2.840.113549.1.1.1-2048.apk META-INF/CERT.RSA. The certs
        // (in the order of appearance in the file) are thus: rsa-1024, rsa-2048. The package's
        // signing cert is rsa-2048.
        ApkVerifier.Result result = verify("v1-only-pkcs7-cert-bag-first-cert-not-used.apk");
        assertVerified(result);
        List<X509Certificate> signingCerts = result.getSignerCertificates();
        assertEquals(1, signingCerts.size());
        assertEquals(
                "fb5dbd3c669af9fc236c6991e6387b7f11ff0590997f22d0f5c74ff40e04fca8",
                HexEncoding.encode(sha256(signingCerts.get(0).getEncoded())));
    }

    @Test
    public void testV1SchemeSignatureCertNotReencoded() throws Exception {
        // Regression test for b/30148997 and b/18228011. When PackageManager does not preserve the
        // original encoded form of signing certificates, bad things happen, such as rejection of
        // completely valid updates to apps. The issue in b/30148997 and b/18228011 was that
        // PackageManager started re-encoding signing certs into DER. This normally produces exactly
        // the original form because X.509 certificates are supposed to be DER-encoded. However, a
        // small fraction of Android apps uses X.509 certificates which are not DER-encoded. For
        // such apps, re-encoding into DER changes the serialized form of the certificate, creating
        // a mismatch with the serialized form stored in the PackageManager database, leading to the
        // rejection of updates for the app.
        //
        // v1-only-with-rsa-1024-cert-not-der.apk cert's signature is not DER-encoded. It is
        // BER-encoded, with length encoded as two bytes instead of just one.
        // v1-only-with-rsa-1024-cert-not-der.apk META-INF/CERT.RSA was obtained from
        // v1-only-with-rsa-1024.apk META-INF/CERT.RSA by manually modifying the ASN.1 structure.
        ApkVerifier.Result result = verify("v1-only-with-rsa-1024-cert-not-der.apk");
        assertVerified(result);
        List<X509Certificate> signingCerts = result.getSignerCertificates();
        assertEquals(1, signingCerts.size());
        assertEquals(
                "c5d4535a7e1c8111687a8374b2198da6f5ff8d811a7a25aa99ef060669342fa9",
                HexEncoding.encode(sha256(signingCerts.get(0).getEncoded())));
    }

    @Test
    public void testMaxSizedZipEocdCommentAccepted() throws Exception {
        // Obtained by modifying apksigner to produce a max-sized (0xffff bytes long) ZIP End of
        // Central Directory comment, and signing the original.apk using the modified apksigner.
        assertVerified(verify("v1-only-max-sized-eocd-comment.apk"));
        assertVerified(
                verifyForMinSdkVersion("v2-only-max-sized-eocd-comment.apk", AndroidSdkVersion.N));
    }

    @Test
    public void testEmptyApk() throws Exception {
        // Unsigned empty ZIP archive
        assertVerificationFailure(
                verifyForMinSdkVersion("empty-unsigned.apk", 1), Issue.JAR_SIG_NO_MANIFEST);

        // JAR-signed empty ZIP archive
        assertVerificationFailure(
                verifyForMinSdkVersion("v1-only-empty.apk", 18),
                Issue.JAR_SIG_NO_SIGNED_ZIP_ENTRIES);

        // APK Signature Scheme v2 signed empty ZIP archive
        assertVerified(verifyForMinSdkVersion("v2-only-empty.apk", AndroidSdkVersion.N));
    }

    private ApkVerifier.Result verify(String apkFilenameInResources)
            throws IOException, ApkFormatException, NoSuchAlgorithmException {
        return verify(apkFilenameInResources, null);
    }

    private ApkVerifier.Result verifyForMinSdkVersion(
            String apkFilenameInResources, int minSdkVersion)
                    throws IOException, ApkFormatException, NoSuchAlgorithmException {
        return verify(apkFilenameInResources, minSdkVersion);
    }

    private ApkVerifier.Result verify(
            String apkFilenameInResources, Integer minSdkVersionOverride)
                    throws IOException, ApkFormatException, NoSuchAlgorithmException {
        byte[] apkBytes = getResourceContents(apkFilenameInResources);

        ApkVerifier.Builder builder =
                new ApkVerifier.Builder(DataSources.asDataSource(ByteBuffer.wrap(apkBytes)));
        if (minSdkVersionOverride != null) {
            builder.setMinCheckedPlatformVersion(minSdkVersionOverride);
        }
        return builder.build().verify();
    }

    private static void assertVerified(ApkVerifier.Result result) {
        if (result.isVerified()) {
            return;
        }

        StringBuilder msg = new StringBuilder();
        for (IssueWithParams issue : result.getErrors()) {
            if (msg.length() > 0) {
                msg.append('\n');
            }
            msg.append(issue);
        }
        for (ApkVerifier.Result.V1SchemeSignerInfo signer : result.getV1SchemeSigners()) {
            String signerName = signer.getName();
            for (IssueWithParams issue : signer.getErrors()) {
                if (msg.length() > 0) {
                    msg.append('\n');
                }
                msg.append("JAR signer ").append(signerName).append(": ").append(issue);
            }
        }
        for (ApkVerifier.Result.V2SchemeSignerInfo signer : result.getV2SchemeSigners()) {
            String signerName = "signer #" + (signer.getIndex() + 1);
            for (IssueWithParams issue : signer.getErrors()) {
                if (msg.length() > 0) {
                    msg.append('\n');
                }
                msg.append("APK Signature Scheme v2 signer ")
                        .append(signerName).append(": ").append(issue);
            }
        }

        fail("APK did not verify: " + msg);
    }

    private void assertVerified(
            String apkFilenameInResources, Integer minSdkVersionOverride) throws Exception {
        assertVerified(verify(apkFilenameInResources, minSdkVersionOverride));
    }

    private static void assertVerificationFailure(ApkVerifier.Result result, Issue expectedIssue) {
        if (result.isVerified()) {
            fail("APK verification succeeded instead of failing with " + expectedIssue);
            return;
        }

        StringBuilder msg = new StringBuilder();
        for (IssueWithParams issue : result.getErrors()) {
            if (expectedIssue.equals(issue.getIssue())) {
                return;
            }
            if (msg.length() > 0) {
                msg.append('\n');
            }
            msg.append(issue);
        }
        for (ApkVerifier.Result.V1SchemeSignerInfo signer : result.getV1SchemeSigners()) {
            String signerName = signer.getName();
            for (ApkVerifier.IssueWithParams issue : signer.getErrors()) {
                if (expectedIssue.equals(issue.getIssue())) {
                    return;
                }
                if (msg.length() > 0) {
                    msg.append('\n');
                }
                msg.append("JAR signer ").append(signerName).append(": ").append(issue);
            }
        }
        for (ApkVerifier.Result.V2SchemeSignerInfo signer : result.getV2SchemeSigners()) {
            String signerName = "signer #" + (signer.getIndex() + 1);
            for (IssueWithParams issue : signer.getErrors()) {
                if (expectedIssue.equals(issue.getIssue())) {
                    return;
                }
                if (msg.length() > 0) {
                    msg.append('\n');
                }
                msg.append("APK Signature Scheme v2 signer ")
                        .append(signerName).append(": ").append(issue);
            }
        }

        fail("APK failed verification for the wrong reason"
                + " . Expected: " + expectedIssue + ", actual: " + msg);
    }

    private void assertVerificationFailure(
            String apkFilenameInResources, ApkVerifier.Issue expectedIssue)
                    throws Exception {
        assertVerificationFailure(verify(apkFilenameInResources), expectedIssue);
    }

    private void assertVerifiedForEach(
            String apkFilenamePatternInResources, String[] args) throws Exception {
        assertVerifiedForEach(apkFilenamePatternInResources, args, null);
    }

    private void assertVerifiedForEach(
            String apkFilenamePatternInResources, String[] args, Integer minSdkVersionOverride)
                    throws Exception {
        for (String arg : args) {
            String apkFilenameInResources =
                    String.format(Locale.US, apkFilenamePatternInResources, arg);
            assertVerified(apkFilenameInResources, minSdkVersionOverride);
        }
    }

    private void assertVerifiedForEachForMinSdkVersion(
            String apkFilenameInResources, String[] args, int minSdkVersion)
                    throws Exception {
        assertVerifiedForEach(apkFilenameInResources, args, minSdkVersion);
    }

    private byte[] getResourceContents(String resourceName) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceName);
            }
            return toByteArray(in);
        }
    }

    private X509Certificate getCertificateFromResources(String resourceName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceName);
            }
            return (X509Certificate)
                    CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    private static byte[] sha256(byte[] msg) throws Exception {
        try {
            return MessageDigest.getInstance("SHA-256").digest(msg);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create SHA-256 MessageDigest", e);
        }
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int chunkSize;
        while ((chunkSize = in.read(buf)) != -1) {
            result.write(buf, 0, chunkSize);
        }
        return result.toByteArray();
    }

    private static void assumeThatRsaPssAvailable() throws Exception {
        Assume.assumeTrue(Security.getProviders("Signature.SHA256withRSA/PSS") != null);
    }
}