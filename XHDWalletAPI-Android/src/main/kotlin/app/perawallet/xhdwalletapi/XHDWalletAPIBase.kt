/*
 * Copyright (c) Algorand Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.perawallet.xhdwalletapi

import com.fasterxml.jackson.databind.ObjectMapper
import app.perawallet.lazysodium.LazySodium
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.msgpack.jackson.dataformat.MessagePackFactory

val CHAIN_CODE_SIZE = 32
val ED25519_SCALAR_SIZE = 32
val ED25519_POINT_SIZE = 32
val INDEX_SIZE = 4

abstract class XHDWalletAPIBase(private var seed: ByteArray) {
  abstract val lazySodium: LazySodium
  companion object {
    val prefixes =
            listOf(
                    "appID",
                    "arc",
                    "aB",
                    "aD",
                    "aO",
                    "aP",
                    "aS",
                    "AS",
                    "BH",
                    "B256",
                    "BR",
                    "CR",
                    "GE",
                    "KP",
                    "MA",
                    "MB",
                    "MX",
                    "NIC",
                    "NIR",
                    "NIV",
                    "NPR",
                    "OT1",
                    "OT2",
                    "PF",
                    "PL",
                    "Program",
                    "ProgData",
                    "PS",
                    "PK",
                    "SD",
                    "SpecialAddr",
                    "STIB",
                    "spc",
                    "spm",
                    "spp",
                    "sps",
                    "spv",
                    "TE",
                    "TG",
                    "TL",
                    "TX",
                    "VO"
            )

    /**
     * Harden a number (set the highest bit to 1) Note that the input is UInt and the output is also
     * UInt
     *
     * @param num
     * @returns
     */
    fun harden(num: UInt): UInt = 0x80000000.toUInt() + num

    /*
     * Get the BIP44 path from the context, account and keyIndex
     *
     * @param context
     * @param account
     * @param keyIndex
     * @returns
     */

    fun getBIP44PathFromContext(
            context: KeyContext,
            account: UInt,
            change: UInt,
            keyIndex: UInt
    ): List<UInt> {
      return when (context) {
        KeyContext.Address -> listOf(harden(44u), harden(283u), harden(account), change, keyIndex)
        KeyContext.Identity -> listOf(harden(44u), harden(0u), harden(account), change, keyIndex)
      }
    }

    /**
     * Implementation how to validate data with encoding and schema, using base64 as an example
     *
     * @param message
     * @param metadata
     * @returns
     */
    fun validateData(message: ByteArray, metadata: SignMetadata): Boolean {
      // Check for Algorand tags
      if (hasAlgorandTags(message)) {
        throw DataValidationException("Data contains Algorand tags")
      }

      val decoded: ByteArray =
              when (metadata.encoding) {
                Encoding.BASE64 -> Base64.getDecoder().decode(message)
                // Encoding.CBOR -> // CBOR is not yet supported
                // across all platforms
                //         ObjectMapper()
                //                 .writeValueAsString(
                //
                // CBORMapper().readValue(message, Map::class.java)
                //                 )
                //                 .toByteArray()
                Encoding.MSGPACK ->
                        ObjectMapper()
                                .writeValueAsString(
                                        ObjectMapper(MessagePackFactory())
                                                .readValue(message, Map::class.java)
                                )
                                .toByteArray()
                Encoding.NONE -> message
              }

      // Validate with schema
      try {
        return metadata.schema.validateBasic(String(decoded)).valid
      } catch (e: Exception) {
        return false
      }
    }

    fun hasAlgorandTags(message: ByteArray): Boolean {
      // Prefixes taken from go-algorand node software code
      // https://github.com/algorand/go-algorand/blob/master/protocol/hash.go

      val messageString = String(message)
      return prefixes.any { messageString.startsWith(it) }
    }

    /**
     * Reference of BIP32-Ed25519 Hierarchical Deterministic Keys over a Non-linear Keyspace
     *
     * @see section V. BIP32-Ed25519: Specification;
     *
     * A) Root keys
     *
     * @param seed
     * - 256 bite seed generated from BIP39 Mnemonic
     * @returns
     * - Extended root key (kL, kR, c) where kL is the left 32 bytes of the root key, kR is the
     * right 32 bytes of the root key, and c is the chain code. Total 96 bytes
     */
    fun fromSeed(seed: ByteArray): ByteArray {
      // k = H512(seed)
      var k = MessageDigest.getInstance("SHA-512").digest(seed)
      var kL = k.sliceArray(0 until ED25519_SCALAR_SIZE)
      var kR = k.sliceArray(ED25519_SCALAR_SIZE until 2 * ED25519_SCALAR_SIZE)

      // While the third highest bit of the last byte of kL is not zero
      while (kL[31].toInt() and 0b00100000 != 0) {
        val hmac = Mac.getInstance("HmacSHA512")
        hmac.init(SecretKeySpec(kL, "HmacSHA512"))
        k = hmac.doFinal(kR)
        kL = k.sliceArray(0 until ED25519_SCALAR_SIZE)
        kR = k.sliceArray(ED25519_SCALAR_SIZE until 2 * ED25519_SCALAR_SIZE)
      }

      // clamp
      // Set the bits in kL as follows:
      // little Endianess
      kL[0] = (kL[0].toInt() and 0b11111000).toByte() // the lowest 3 bits of the first
      // byte of kL are cleared
      kL[31] = (kL[31].toInt() and 0b01111111).toByte() // the highest bit of the last
      // byte is cleared
      kL[31] = (kL[31].toInt() or 0b01000000).toByte() // the second highest bit of the
      // last byte is set

      // chain root code
      // SHA256(0x01||k)
      val c = MessageDigest.getInstance("SHA-256").digest(byteArrayOf(0x01) + seed)
      return kL + kR + c
    }
  }

  /**
   *
   * @see section V. BIP32-Ed25519: Specification
   *
   * @param kl
   * - The scalar
   * @param cc
   * - chain code
   * @param index
   * - non-hardened ( < 2^31 ) index
   * @returns
   * - (z, c) where z is the 64-byte child key and c is the chain code
   */
  open fun deriveNonHardened(
          kl: ByteArray,
          cc: ByteArray,
          index: UInt
  ): Pair<ByteArray, ByteArray> {
    val data = ByteBuffer.allocate(1 + ED25519_SCALAR_SIZE + INDEX_SIZE)
    data.put(1 + ED25519_SCALAR_SIZE, index.toByte())

    val pk = lazySodium.cryptoScalarMultEd25519BaseNoclamp(kl).toBytes()
    data.position(1)
    data.put(pk)

    data.put(0, 0x02)
    val hmac = Mac.getInstance("HmacSHA512")
    hmac.init(SecretKeySpec(cc, "HmacSHA512"))
    val z = hmac.doFinal(data.array())

    data.put(0, 0x03)
    hmac.init(SecretKeySpec(cc, "HmacSHA512"))
    val fullChildChainCode = hmac.doFinal(data.array())
    val childChainCode = fullChildChainCode.sliceArray(CHAIN_CODE_SIZE until 2 * CHAIN_CODE_SIZE)

    return Pair(z, childChainCode)
  }

  /**
   *
   * @see section V. BIP32-Ed25519: Specification
   *
   * @param kl
   * - The scalar (a.k.a private key)
   * @param kr
   * - the right 32 bytes of the root key
   * @param cc
   * - chain code
   * @param index
   * - hardened ( >= 2^31 ) index
   * @returns
   * - (z, c) where z is the 64-byte child key and c is the chain code
   */
  fun deriveHardened(
          kl: ByteArray,
          kr: ByteArray,
          cc: ByteArray,
          index: UInt
  ): Pair<ByteArray, ByteArray> {
    val indexLEBytes = ByteArray(4) { i -> ((index shr (8 * i)) and 0xFFu).toByte() }
    val data = ByteBuffer.allocate(1 + 2 * ED25519_SCALAR_SIZE + INDEX_SIZE)
    data.position(1 + 2 * ED25519_SCALAR_SIZE)
    data.put(indexLEBytes)
    data.position(1)
    data.put(kl)
    data.put(kr)

    data.put(0, 0x00)
    val hmac = Mac.getInstance("HmacSHA512")
    hmac.init(SecretKeySpec(cc, "HmacSHA512"))
    val z = hmac.doFinal(data.array())

    data.put(0, 0x01)
    hmac.init(SecretKeySpec(cc, "HmacSHA512"))
    val fullChildChainCode = hmac.doFinal(data.array())
    val childChainCode = fullChildChainCode.sliceArray(CHAIN_CODE_SIZE until 2 * CHAIN_CODE_SIZE)

    return Pair(z, childChainCode)
  }

  /**
   * @see section V. BIP32-Ed25519: Specification;
   *
   * subsections:
   *
   * B) Child Keys and C) Private Child Key Derivation
   *
   * @param extendedKey
   * - extended key (kL, kR, c) where kL is the left 32 bytes of the root key the scalar (pvtKey).
   * kR is the right 32 bytes of the root key, and c is the chain code. Total 96 bytes
   * @param index
   * - index of the child key
   * @returns
   * - (kL, kR, c) where kL is the left 32 bytes of the child key (the new scalar), kR is the right
   * 32 bytes of the child key, and c is the chain code. Total 96 bytes
   */
  public fun deriveChildNodePrivate(
          extendedKey: ByteArray,
          index: UInt,
          derivationType: Bip32DerivationType = Bip32DerivationType.Peikert
  ): ByteArray {
    val kl = extendedKey.sliceArray(0 until ED25519_SCALAR_SIZE)
    val kr = extendedKey.sliceArray(ED25519_SCALAR_SIZE until 2 * ED25519_SCALAR_SIZE)
    val cc =
            extendedKey.sliceArray(
                    2 * ED25519_SCALAR_SIZE until 2 * ED25519_SCALAR_SIZE + CHAIN_CODE_SIZE
            )

    val (z, chainCode) =
            if (index < 0x80000000.toUInt()) deriveNonHardened(kl, cc, index)
            else deriveHardened(kl, kr, cc, index)

    val zl = z.sliceArray(0 until ED25519_SCALAR_SIZE)
    val zr = z.sliceArray(ED25519_SCALAR_SIZE until 2 * ED25519_SCALAR_SIZE)

    // left = kl + 8 * trunc28(zl)
    // right = zr + kr

    // Note: Right is taken mod 2^256 in the original BIP32 ED25519 paper:
    // V. BIP32-ED25519: SPECIFICATION
    // C. Private Child
    // Equation 2
    // However, left is NOT specified to be taken mod 2^256

    val leftBigInteger =
            (BigInteger(1, kl.reversedArray())
                    .plus(
                            BigInteger(
                                            1,
                                            trunc256MinusGBits(zl.clone(), derivationType.value)
                                                    .reversedArray()
                                    )
                                    .multiply(BigInteger.valueOf(8L))
                    ))

    if (leftBigInteger >= BigInteger.valueOf(2).pow(255)) {
      throw BigIntegerOverflowException()
    }

    val left =
            leftBigInteger.toByteArray().reversedArray().let { bytes ->
              if (bytes.size > ED25519_SCALAR_SIZE) {
                throw BigIntegerOverflowException()
              } else {
                ByteArray(ED25519_SCALAR_SIZE - bytes.size) + bytes
              }
            } // Pad to 32 bytes

    var right =
            (BigInteger(1, kr.reversedArray()) + BigInteger(1, zr.reversedArray()))
                    .toByteArray()
                    .reversedArray()
                    .let { bytes ->
                      bytes.sliceArray(0 until minOf(bytes.size, ED25519_SCALAR_SIZE))
                    } // Slice to 32 bytes

    right = right + ByteArray((ED25519_SCALAR_SIZE - right.size + 32) % 32)

    return ByteBuffer.allocate(ED25519_SCALAR_SIZE + ED25519_SCALAR_SIZE + CHAIN_CODE_SIZE)
            .put(left)
            .put(right)
            .put(chainCode)
            .array()
  }

  /**
   * * @see section V. BIP32-Ed25519: Specification;
   *
   * subsections:
   *
   * D) Public Child key
   *
   * @param extendedKey
   * - extend public key (p, c) where p is the public key and c is the chain code. Total 64 bytes
   * @param index
   * - unharden index (i < 2^31) of the child key
   * @param g
   * - Defines how many bits to zero in the left 32 bytes of the child key. Standard BIP32-ed25519
   * derivations use 32 bits.
   * @returns
   * - 64 bytes, being the 32 bytes of the child key (the new public key) followed by the 32 bytes
   * of the chain code
   */
  public fun deriveChildNodePublic(
          extendedKey: ByteArray,
          index: UInt,
          derivationType: Bip32DerivationType = Bip32DerivationType.Peikert
  ): ByteArray {
    if (index > 0x80000000u)
            throw IllegalArgumentException("Cannot derive public key with hardened index")

    val pk = extendedKey.sliceArray(0 until ED25519_POINT_SIZE)
    val cc = extendedKey.sliceArray(ED25519_POINT_SIZE until ED25519_POINT_SIZE + CHAIN_CODE_SIZE)

    // Step 1: Compute Z
    val data = ByteBuffer.allocate(1 + ED25519_SCALAR_SIZE + 4)
    data.put(1 + ED25519_SCALAR_SIZE, index.toByte())

    data.position(1)
    data.put(pk)

    data.put(0, 0x02)
    val hmac = Mac.getInstance("HmacSHA512")
    hmac.init(SecretKeySpec(cc, "HmacSHA512"))
    val z = hmac.doFinal(data.array())
    val zl = trunc256MinusGBits(z.sliceArray(0 until ED25519_SCALAR_SIZE), derivationType.value)

    // Step 2: Compute child PK

    /*
    ######################################
    Standard BIP32-ed25519 derivation
    #######################################
    zL = 8 * 28bytesOf(z_left_hand_side)

    ######################################
    Chris Peikert's ammendment to BIP32-ed25519 derivation
    #######################################
    zL = 8 * trunc_256_minus_g_bits (z_left_hand_side, g)
    */

    val left =
            (BigInteger(1, zl.reversedArray()) * BigInteger.valueOf(8L))
                    .toByteArray()
                    .reversedArray()
                    .let { bytes ->
                      bytes + ByteArray(ED25519_SCALAR_SIZE - bytes.size)
                    } // Pad to 32 bytes

    val p = lazySodium.cryptoScalarMultEd25519BaseNoclamp(left).toBytes()

    // Step 3: Compute child chain code
    data.put(0, 0x03)
    hmac.init(SecretKeySpec(cc, "HmacSHA512"))
    val fullChildChainCode = hmac.doFinal(data.array())
    val childChainCode = fullChildChainCode.sliceArray(CHAIN_CODE_SIZE until 2 * CHAIN_CODE_SIZE)

    val newPK = ByteArray(32)
    lazySodium.cryptoCoreEd25519Add(newPK, p, pk)

    return ByteBuffer.allocate(ED25519_POINT_SIZE + CHAIN_CODE_SIZE)
            .put(newPK)
            .put(childChainCode)
            .array()
  }

  /** z_L by */
  internal fun trunc256MinusGBits(zl: ByteArray, g: Int): ByteArray {
    if (g < 0 || g > 256) {
      throw IllegalArgumentException("Number of bits to zero must be between 0 and 256.")
    }

    val truncated = zl
    var remainingBits = g

    // start from the last byte and move backwards
    for (i in truncated.size - 1 downTo 0) {
      if (remainingBits >= 8) {
        truncated[i] = 0
        remainingBits -= 8
      } else {
        val mask = ((1 shl (8 - remainingBits)) - 1).toByte()
        truncated[i] = (truncated[i].toInt() and mask.toInt()).toByte()
        break
      }
    }
    return truncated
  }

  /**
   * Derives a child key from the root key based on BIP44 path
   *
   * @param rootKey
   * - root key in extended format (kL, kR, c). It should be 96 bytes long
   * @param bip44Path
   * - BIP44 path (m / purpose' / coin_type' / account' / change / address_index). The ' indicates
   * that the value is hardened
   * @param isPrivate
   * - returns full 64 bytes privatekey (first 32 bytes scalar), false returns 32 byte public key,
   * @returns
   * - The public key of 32 bytes. If isPrivate is true, returns the private key instead.
   */
  public fun deriveKey(
          rootKey: ByteArray,
          bip44Path: List<UInt>,
          isPrivate: Boolean,
          derivationType: Bip32DerivationType = Bip32DerivationType.Peikert
  ): ByteArray {
    var derived = rootKey
    for (path in bip44Path) {
      derived = this.deriveChildNodePrivate(derived, path, derivationType)
    }
    if (isPrivate) {
      return derived
    } else {
      return lazySodium
              .cryptoScalarMultEd25519BaseNoclamp(derived.sliceArray(0 until ED25519_SCALAR_SIZE))
              .toBytes() +
              derived.sliceArray(
                      2 * ED25519_SCALAR_SIZE until 2 * ED25519_SCALAR_SIZE + CHAIN_CODE_SIZE
              )
    }
  }

  /**
   *
   * @param context
   * - context of the key (i.e Address, Identity)
   * @param account
   * - account number. This value will be hardened as part of BIP44
   * @param keyIndex
   * - key index. This value will be a SOFT derivation as part of BIP44.
   * @returns
   * - public key 32 bytes
   */
  fun keyGen(
          context: KeyContext,
          account: UInt,
          change: UInt,
          keyIndex: UInt,
          derivationType: Bip32DerivationType = Bip32DerivationType.Peikert
  ): ByteArray {
    val rootKey: ByteArray = fromSeed(this.seed)
    val bip44Path: List<UInt> = getBIP44PathFromContext(context, account, change, keyIndex)
    return this.deriveKey(rootKey, bip44Path, false, derivationType)
            .sliceArray(0 until ED25519_POINT_SIZE)
  }

  /**
   * Sign arbitrary but non-Algorand related data
   * @param context
   * - context of the key (i.e Address, Identity)
   * @param account
   * - account number. This value will be hardened as part of BIP44
   * @param keyIndex
   * - key index. This value will be a SOFT derivation as part of BIP44.
   * @param data
   * - data to be signed in raw bytes
   * @param metadata
   * - metadata object that describes how `data` was encoded and what schema to use to validate
   * against
   *
   * @returns
   * - signature holding R and S, totally 64 bytes
   */
  fun signData(
          context: KeyContext,
          account: UInt,
          change: UInt,
          keyIndex: UInt,
          data: ByteArray,
          metadata: SignMetadata,
          derivationType: Bip32DerivationType = Bip32DerivationType.Peikert
  ): ByteArray {

    val valid = validateData(data, metadata)

    if (!valid) { // failed schema validation
      throw DataValidationException("Data validation failed")
    }

    return rawSign(
            getBIP44PathFromContext(context, account, change, keyIndex),
            data,
            derivationType
    )
  }

  /**
   * Sign Algorand transaction
   * @param context
   * - context of the key (i.e Address, Identity)
   * @param account
   * - account number. This value will be hardened as part of BIP44
   * @param keyIndex
   * - key index. This value will be a SOFT derivation as part of BIP44.
   * @param tx
   * - Transaction object containing parameters to be signed, e.g. sender, receiver, amount, fee,
   *
   * @returns stx
   * - SignedTransaction object
   */
  fun signAlgoTransaction(
          context: KeyContext,
          account: UInt,
          change: UInt,
          keyIndex: UInt,
          prefixEncodedTx: ByteArray,
          derivationType: Bip32DerivationType = Bip32DerivationType.Peikert
  ): ByteArray {
    return rawSign(
            getBIP44PathFromContext(context, account, change, keyIndex),
            prefixEncodedTx,
            derivationType
    )
  }

  /**
   * Raw Signing function called by signData and signTransaction
   *
   * Ref: https://datatracker.ietf.org/doc/html/rfc8032#section-5.1.6
   *
   * Edwards-Curve Digital Signature Algorithm (EdDSA)
   *
   * @param bip44Path
   * - BIP44 path (m / purpose' / coin_type' / account' / change / address_index)
   * @param data
   * - data to be signed in raw bytes
   *
   * @returns
   * - signature holding R and S, totally 64 bytes
   */
  fun rawSign(
          bip44Path: List<UInt>,
          data: ByteArray,
          derivationType: Bip32DerivationType = Bip32DerivationType.Peikert
  ): ByteArray {

    val rootKey: ByteArray = fromSeed(this.seed)
    val raw: ByteArray = deriveKey(rootKey, bip44Path, true, derivationType)

    val scalar = raw.sliceArray(0 until 32)
    val c = raw.sliceArray(32 until 64)

    // \(1): pubKey = scalar * G (base point, no clamp)
    val publicKey = lazySodium.cryptoScalarMultEd25519BaseNoclamp(scalar).toBytes()

    // \(2): r = hash(c + msg) mod q [LE]
    var r = this.safeModQ(MessageDigest.getInstance("SHA-512").digest(c + data))

    // \(4):  R = r * G (base point, no clamp)
    val R = lazySodium.cryptoScalarMultEd25519BaseNoclamp(r).toBytes()

    var h = this.safeModQ(MessageDigest.getInstance("SHA-512").digest(R + publicKey + data))

    // \(5): S = (r + h * k) mod q
    var S =
            this.safeModQ(
                    lazySodium.cryptoCoreEd25519ScalarAdd(
                            r,
                            lazySodium
                                    .cryptoCoreEd25519ScalarMul(h, scalar)
                                    .toByteArray()
                                    .reversedArray()
                    )
            )

    return R + S
  }

  /*
   * SafeModQ is a helper function to ensure that the result of a mod q operation is 32 bytes
   * It wraps around the cryptoCoreEd25519ScalarReduce function, which can accept either BigInteger or ByteArray
   */
  fun safeModQ(input: BigInteger): ByteArray {
    var reduced = lazySodium.cryptoCoreEd25519ScalarReduce(input).toByteArray().reversedArray()
    if (reduced.size < 32) {
      reduced = reduced + ByteArray(32 - reduced.size)
    }
    return reduced
  }

  fun safeModQ(input: ByteArray): ByteArray {
    var reduced = lazySodium.cryptoCoreEd25519ScalarReduce(input).toByteArray().reversedArray()
    if (reduced.size < 32) {
      reduced = reduced + ByteArray(32 - reduced.size)
    }
    return reduced
  }

  /**
   * Wrapper around libsodium basic signature verification
   *
   * Any lib or system that can verify EdDSA signatures can be used
   *
   * @param signature
   * - raw 64 bytes signature (R, S)
   * @param message
   * - raw bytes of the message
   * @param publicKey
   * - raw 32 bytes public key (x,y)
   * @returns true if signature is valid, false otherwise
   */
  fun verifyWithPublicKey(signature: ByteArray, message: ByteArray, publicKey: ByteArray): Boolean {
    return lazySodium.cryptoSignVerifyDetached(signature, message, message.size, publicKey)
  }

  /**
   * Function to perform ECDH against a provided public key
   *
   * ECDH reference link: https://en.wikipedia.org/wiki/Elliptic-curve_Diffie%E2%80%93Hellman
   *
   * It creates a shared secret between two parties. Each party only needs to be aware of the
   * other's public key. This symmetric secret can be used to derive a symmetric key for encryption
   * and decryption. Creating a private channel between the two parties. Note that you must specify
   * the order of concatenation for the public keys with otherFirst.
   * @param context
   * - context of the key (i.e Address, Identity)
   * @param account
   * - account number. This value will be hardened as part of BIP44
   * @param keyIndex
   * - key index. This value will be a SOFT derivation as part of BIP44.
   * @param otherPartyPub
   * - raw 32 bytes public key of the other party
   * @param meFirst
   * - decide the order of concatenation of the public keys in the shared secret, true: my public
   * key first, false: other party's public key first
   * @returns
   * - raw 32 bytes shared secret
   */
  fun ECDH(
          context: KeyContext,
          account: UInt,
          change: UInt,
          keyIndex: UInt,
          otherPartyPub: ByteArray,
          meFirst: Boolean,
          derivationType: Bip32DerivationType = Bip32DerivationType.Peikert
  ): ByteArray {

    val rootKey: ByteArray = fromSeed(this.seed)

    val publicKey: ByteArray = this.keyGen(context, account, change, keyIndex, derivationType)
    val privateKey: ByteArray =
            this.deriveKey(
                    rootKey,
                    getBIP44PathFromContext(context, account, change, keyIndex),
                    true,
                    derivationType
            )

    val scalar: ByteArray = privateKey.sliceArray(0 until 32)

    val sharedPoint = ByteArray(32)
    val myCurve25519Key = ByteArray(32)
    val otherPartyCurve25519Key = ByteArray(32)

    lazySodium.convertPublicKeyEd25519ToCurve25519(myCurve25519Key, publicKey)
    lazySodium.convertPublicKeyEd25519ToCurve25519(otherPartyCurve25519Key, otherPartyPub)
    lazySodium.cryptoScalarMult(sharedPoint, scalar, otherPartyCurve25519Key)

    val concatenated: ByteArray

    if (meFirst) {
      concatenated = sharedPoint + myCurve25519Key + otherPartyCurve25519Key
    } else {
      concatenated = sharedPoint + otherPartyCurve25519Key + myCurve25519Key
    }

    val output = ByteArray(32)
    lazySodium.cryptoGenericHash(
            output,
            32,
            concatenated,
            concatenated.size.toLong(),
    )
    return output
  }
}
