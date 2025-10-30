/*
 * Copyright (c) Terl Tech Ltd • 01/04/2021, 12:31 • goterl.com
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package app.perawallet.lazysodium.utils;

import app.perawallet.lazysodium.Sodium;
import app.perawallet.lazysodium.interfaces.MessageEncoder;

import java.util.Base64;

public class Base64MessageEncoder implements MessageEncoder {

    @Override
    public String encode(byte[] cipher) {
        return Sodium.base64Facade.encode(cipher);
    }

    @Override
    public byte[] decode(String cipherText) {
        return Sodium.base64Facade.decode(cipherText);
    }
}
