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

import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.imageio.ImageWriteParam;
import javax.imageio.spi.ImageWriterSpi;

import org.dcm4che3.imageio.codec.ImageDescriptor;
import org.opencv.core.CvType;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.op.ImageConversion;

/**
 * @author Nicolas Roduit
 * @since Mar 2025
 */
class NativeJXLImageWriter extends AbstractNativeImageWriter {

    NativeJXLImageWriter(ImageWriterSpi originatingProvider) throws IOException {
        super(originatingProvider);
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return new JXLImageWriteParam(getLocale());
    }

    @Override
    String codecName() {
        return "JPEG XL";
    }

    @Override
    void validate(ImageWriteParam param, ImageDescriptor desc) {
        rejectChromaSubsampledLossless(param.isCompressionLossless(),
            desc.getPhotometricInterpretation());
    }

    @Override
    ImageCV toMat(RenderedImage image, ImageWriteParam param, ImageDescriptor desc) {
        // JXL codec requires BGR or Gray
        return ImageConversion.toMat(image, param.getSourceRegion(), true);
    }

    @Override
    MatOfInt buildDicomParams(ImageCV mat, RenderedImage image, ImageWriteParam param, ImageDescriptor desc) {
        JXLImageWriteParam jxlParams = (JXLImageWriteParam) param;
        int bitCompressed = ((desc.getBitsCompressed() + 7) / 8) * 8; // round up to whole bytes
        int channels = CvType.channels(mat.type());
        int dcmFlags = desc.isSigned() ? Imgcodecs.DICOM_FLAG_SIGNED : Imgcodecs.DICOM_FLAG_UNSIGNED;

        int[] params = new int[18]; // Extended for JXL parameters
        params[Imgcodecs.DICOM_PARAM_IMREAD] = Imgcodecs.IMREAD_UNCHANGED; // Image flags
        params[Imgcodecs.DICOM_PARAM_DCM_IMREAD] = dcmFlags; // DICOM flags
        params[Imgcodecs.DICOM_PARAM_WIDTH] = mat.width(); // Image width
        params[Imgcodecs.DICOM_PARAM_HEIGHT] = mat.height(); // Image height
        params[Imgcodecs.DICOM_PARAM_COMPRESSION] = Imgcodecs.DICOM_CP_JXL; // Type of compression
        params[Imgcodecs.DICOM_PARAM_COMPONENTS] = channels; // Number of components
        params[Imgcodecs.DICOM_PARAM_BITS_PER_SAMPLE] = bitCompressed; // Bits per sample
        params[Imgcodecs.DICOM_PARAM_INTERLEAVE_MODE] = Imgcodecs.ILV_SAMPLE; // Interleave mode
        params[Imgcodecs.DICOM_PARAM_COLOR_MODEL] = monochromeOrRgb(channels); // Photometric interpretation
        params[Imgcodecs.DICOM_PARAM_JPEG_QUALITY] = (int) (jxlParams.getEffectiveQuality() * 100); // Quality (0-100)
        params[Imgcodecs.DICOM_PARAM_JXL_EFFORT] = jxlParams.getEffort(); // Effort (1-9)
        params[Imgcodecs.DICOM_PARAM_JXL_DECODING_SPEED] = jxlParams.getDecodingSpeed(); // Decoding speed (0-4, 0 = best quality/density)
        return new MatOfInt(params);
    }
}
