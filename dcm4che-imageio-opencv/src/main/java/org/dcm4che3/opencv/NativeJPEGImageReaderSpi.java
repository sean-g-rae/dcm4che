/*
 * **** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * J4Care.
 * Portions created by the Initial Developer are Copyright (C) 2015-2018
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * **** END LICENSE BLOCK *****
 *
 */

package org.dcm4che3.opencv;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * @author Nicolas Roduit
 * @since Mar 2018
 */
public class NativeJPEGImageReaderSpi extends ImageReaderSpi {

    static final String[] NAMES = { "jpeg-cv" };
    static final String[] SUFFIXES = {};
    static final String[] MIMES = {};

    public NativeJPEGImageReaderSpi() {
        super("Weasis Team", "1.0", NAMES, SUFFIXES, MIMES, NativeImageReader.class.getName(),
            new Class[] { ImageInputStream.class }, new String[] {  }, false, // supportsStandardStreamMetadataFormat
            null, // nativeStreamMetadataFormatName
            null, // nativeStreamMetadataFormatClassName
            null, // extraStreamMetadataFormatNames
            null, // extraStreamMetadataFormatClassNames
            false, // supportsStandardImageMetadataFormat
            null, null, null, null);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Natively-accelerated JPEG Image Reader (8/12/16 bits, IJG 6b based)";
    }

    @Override
    public String[] getFileSuffixes() {
        return SUFFIXES;
    }

    @Override
    public String[] getMIMETypes() {
        return MIMES;
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        // NativeImageReader.read() eventually instantiates a StreamSegment,
        // which does not support all ImageInputStreams
        if (!StreamSegment.supportsInputStream(source)) {
            return false;
        }
        ImageInputStream iis = (ImageInputStream) source;

        iis.mark();
        try {
            int byte1 = iis.read();
            int byte2 = iis.read();
            // Magic numbers for JPEG (general jpeg marker)
            if ((byte1 != 0xFF) || (byte2 != 0xD8)) {
                return false;
            }
            do {
                byte1 = iis.read();
                byte2 = iis.read();
                // Something wrong, but try to read it anyway
                if (byte1 != 0xFF) {
                    break;
                }
                // Start of scan
                if (byte2 == 0xDA) {
                    break;
                }
                // SOF55 (0xF7) marks a JPEG-LS file, which is not supported by this reader.
                if (byte2 == 0xF7) {
                    return false;
                }
                // Any JPEG Start-Of-Frame marker means this reader can decode the stream.
                if (NativeImageReader.isSOFMarker(byte2)) {
                    return true;
                }
                int length = iis.read() << 8;
                length += iis.read();
                length -= 2;
                while (length > 0) {
                    length -= iis.skipBytes(length);
                }
            } while (true);
            return true;
        } finally {
            iis.reset();
        }
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IIOException {
        return new NativeImageReader(this, false);
    }
}
