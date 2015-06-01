package omed.bf

import java.security.cert.{X509Certificate, CertificateFactory, Certificate}
import com.objsys.asn1j.runtime._
import ru.CryptoPro.JCP.ASN.CryptographicMessageSyntax.{SignerInfo, DigestAlgorithmIdentifier, SignedData, ContentInfo}
import ru.CryptoPro.JCP.params.OID
import ru.CryptoPro.JCP.ASN.PKIX1Explicit88.Attribute
import java.security.{Security, DigestInputStream, MessageDigest, Signature}
import ru.CryptoPro.JCP.JCP
import java.io.ByteArrayInputStream
import ru.CryptoPro.JCP.tools.Array
import scala.Array
import com.sun.org.apache.xml.internal.security.utils.Base64
import sun.security.x509.X500Name

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 25.11.13
 * Time: 14:34
 * To change this template use File | Settings | File Templates.
 */
object ECPProvider {
  val STR_CMS_OID_SIGNED: String = "1.2.840.113549.1.7.2"
  val STR_CMS_OID_DIGEST_ATTR: String = "1.2.840.113549.1.9.4"
  val STR_CMS_OID_CONT_TYP_ATTR: String = "1.2.840.113549.1.9.3"
  val  STR_CMS_OID_SIGN_TYM_ATTR = "1.2.840.113549.1.9.5"
  val DIGEST_ALG_NAME: String = JCP.GOST_DIGEST_NAME
  val DIGEST_OID: String = JCP.GOST_DIGEST_OID

  def CMSVerify(buffer:scala.Array[Byte], certs: scala.Array[Certificate], data: scala.Array[Byte]):(Boolean,String)= {
    Security.addProvider(new JCP)
    val signdata = Base64.decode(buffer)
    var validsign = 0
    val asnBuf: Asn1BerDecodeBuffer = new Asn1BerDecodeBuffer(signdata)
    val all: ContentInfo = new ContentInfo
    all.decode(asnBuf)
   // if (!new OID(STR_CMS_OID_SIGNED).equals(all.contentType.value)) throw new Exception("Not supported")
    val cms: SignedData = all.content.asInstanceOf[SignedData]

    val text = if (cms.encapContentInfo.eContent != null)  cms.encapContentInfo.eContent.value
    else if (data != null) data
    else throw new Exception("No content for verify")
    var digestOid: OID = null
    val digestAlgorithmIdentifier: DigestAlgorithmIdentifier = new DigestAlgorithmIdentifier(new OID(DIGEST_OID).value)

    var i: Int = 0
    while (i < cms.digestAlgorithms.elements.length) {

      val tmp =cms.digestAlgorithms.elements(i)
      if (cms.digestAlgorithms.elements(i).algorithm == digestAlgorithmIdentifier.algorithm) {
        digestOid = new OID(cms.digestAlgorithms.elements(i).algorithm.value)
        i=cms.digestAlgorithms.elements.length
      }

    }

    if (digestOid == null) throw new Exception("Unknown digest")
    val eContTypeOID: OID = new OID(cms.encapContentInfo.eContentType.value)
    var lastName :String = null
    if (cms.certificates != null) {
      for ( i<- 0 to cms.certificates.elements.length -1) {
        val encBuf = new Asn1BerEncodeBuffer()
        cms.certificates.elements(i).encode(encBuf)

        val cf = CertificateFactory.getInstance("X.509")
        val cert =  cf.generateCertificate(encBuf.getInputStream()).asInstanceOf[X509Certificate]
        lastName = cert.getSubjectDN().asInstanceOf[X500Name].getCommonName
        for (j<- 0 to cms.signerInfos.elements.length-1) {
          val info = cms.signerInfos.elements(j)
          if (!digestOid.equals(new OID(info.digestAlgorithm.algorithm.value)))
            throw new Exception("Not signed on certificate.")
          val checkResult = verifyOnCert(cert,
            cms.signerInfos.elements(j), text, eContTypeOID)
          if(checkResult)  validsign += 1
        }
      }
    }

    // проверка на сертификаты переданные отдельно
    //    if (certs != null) {
    //      for ( i<- (0,certs.length)) {
    //        val  cert = certs(i).asInstanceOf[X509Certificate]
    //        for ( j <-(0,cms.signerInfos.elements.length)) {
    //          val info = cms.signerInfos.elements(j)
    //          if (!digestOid.equals(new OID(
    //            info.digestAlgorithm.algorithm.value)))
    //            throw new Exception("Not signed on certificate.")
    //          val checkResult = verifyOnCert(cert,
    //            cms.signerInfos.elements(j), text, eContTypeOID)
    //   if(checkResult)  validsign += 1
    //        }
    //      }
    //    }
    //    else {
    //      //todo throw exception если нет сертификата
    //    }
    (validsign > 0,lastName)
  }

  /**
   * ������� �������� ������� �� ��������� �����������
   *
   * @param cert ���������� ��� ��������
   * @param text ����� ��� ��������
   * @param info �������
   * @return ����� �� �������
   * @throws Exception ������
   */
  private def verifyOnCert(cert: X509Certificate, info: SignerInfo, text: scala.Array[Byte], eContentTypeOID: OID): Boolean = {
    val sign: scala.Array[Byte] = info.signature.value
    var data: scala.Array[Byte] = null
    if (info.signedAttrs == null) {
      data = text
    }
    else {
      val signAttrElem: scala.Array[Attribute] = info.signedAttrs.elements
      val contentTypeOid: Asn1ObjectIdentifier = new Asn1ObjectIdentifier((new OID(STR_CMS_OID_CONT_TYP_ATTR)).value)
      var contentTypeAttr: Attribute = null
      for ( r  <- 0 to signAttrElem.length-1)   {
        val  oid = signAttrElem(r).`type`
        if (oid.equals(contentTypeOid)) {
          contentTypeAttr = signAttrElem(r)
        }
      }

      if (contentTypeAttr == null) throw new Exception("content-type attribute not present")
      if (!(contentTypeAttr.values.elements(0) == new Asn1ObjectIdentifier(eContentTypeOID.value))) throw new Exception("content-type attribute OID not equal eContentType OID")
      val messageDigestOid: Asn1ObjectIdentifier = new Asn1ObjectIdentifier((new OID(STR_CMS_OID_DIGEST_ATTR)).value)
      var messageDigestAttr: Attribute = null

      for ( r<- 0 to signAttrElem.length-1) {
        val  oid = signAttrElem(r).`type`
        if (oid.equals(messageDigestOid)) {
          messageDigestAttr = signAttrElem(r)
        }
      }

      if (messageDigestAttr == null) throw new Exception("message-digest attribute not present")
      val open: Asn1Type = messageDigestAttr.values.elements(0)
      val hash: Asn1OctetString = open.asInstanceOf[Asn1OctetString]
      val md: scala.Array[Byte] = hash.value
      val dm: scala.Array[Byte] = digestm(text, DIGEST_ALG_NAME)
      if (!(ru.CryptoPro.JCP.tools.Array.toHexString(dm) == ru.CryptoPro.JCP.tools.Array.toHexString(md))) throw new Exception("message-digest attribute verify failed")
      val signTimeOid: Asn1ObjectIdentifier = new Asn1ObjectIdentifier((new OID(STR_CMS_OID_SIGN_TYM_ATTR)).value)
      var signTimeAttr: Attribute = null

      for (r<-0 to signAttrElem.length-1) {
        val  oid = signAttrElem(r).`type`
        if (oid.equals(messageDigestOid)) {
          signTimeAttr = signAttrElem(r)
        }
      }
      if (signTimeAttr != null) {
      }
      val encBufSignedAttr: Asn1BerEncodeBuffer = new Asn1BerEncodeBuffer
      info.signedAttrs.encode(encBufSignedAttr)
      data = encBufSignedAttr.getMsgCopy
    }
    val signature: Signature = Signature.getInstance(JCP.GOST_EL_SIGN_NAME)
    signature.initVerify(cert)
    signature.update(data)
    return signature.verify(sign)
  }

  /**
   * @param bytes bytes
   * @param digestAlgorithmName algorithm
   * @return digest
   * @throws Exception e
   */
  def digestm(bytes: scala.Array[Byte], digestAlgorithmName: String): scala.Array[Byte] = {
    val stream: ByteArrayInputStream = new ByteArrayInputStream(bytes)
    val digest: MessageDigest = MessageDigest.getInstance(digestAlgorithmName)
    val digestStream: DigestInputStream = new DigestInputStream(stream, digest)
    while (digestStream.available != 0) digestStream.read
    return digest.digest
  }
}
