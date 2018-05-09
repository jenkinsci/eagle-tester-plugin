package com.mobileenerlytics.eagle.tester.common.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.util.FormValidation;
import net.nicholaswilliams.java.licensing.*;
import net.nicholaswilliams.java.licensing.encryption.PasswordProvider;
import net.nicholaswilliams.java.licensing.encryption.PublicKeyDataProvider;
import net.nicholaswilliams.java.licensing.exception.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.Date;

public class LicenseVerifier implements PasswordProvider,
        PublicKeyDataProvider, LicenseValidator {

    private static LicenseVerifier mInstance;

    static LicenseVerifier getInstance() {
        if(mInstance == null)
            mInstance = new LicenseVerifier();
        return mInstance;
    }

    private LicenseVerifier() {
        LicenseManagerProperties.setPublicKeyDataProvider(this);
        LicenseManagerProperties.setPublicKeyPasswordProvider(this);

        EagleLicenseProvider provider = new EagleLicenseProvider();
//        provider.setFileOnClasspath(false);
//        provider.setBase64Encoded(true);

//        provider.setFilePrefix(getPrefix());
//        provider.setFileSuffix(".lic");

        LicenseManagerProperties.setLicenseProvider(provider);

        // Optional; set only if you wish to validate licenses
        LicenseManagerProperties.setLicenseValidator(this);

        // Optional; defaults to 0, which translates to a 10-second (minimum)
        // cache time
        LicenseManagerProperties.setCacheTimeInMinutes(5);
    }


    @Override
    public void validateLicense(License license) throws InvalidLicenseException {

    }

    @Override
    public byte[] getEncryptedPublicKeyData() throws KeyNotFoundException {
        try {
            return IOUtils.toByteArray(LicenseVerifier.class.getResourceAsStream(
                    "/enerlytics.public.key"));
        } catch (IOException e) {
            throw new KeyNotFoundException("The public key file was not found.", e);
        }
    }

    @Override
    public char[] getPassword() {
        return "ycndajxc".toCharArray(); //$NON-NLS-1$
    }

    public FormValidation verify(String licenseString, String username) {
        LicenseManager manager = LicenseManager.getInstance();
        try {
            License license = manager.getLicense(licenseString);
            if (license == null)
                return FormValidation.error("License not found.");

            if (!license.hasLicenseForFeature("EAGLE"))
                return FormValidation.error("License isn't enabled for Eagle");

            if (!license.getHolder().equals(username))
                return FormValidation.error("License provided is not for user " + username);

            Date todayDate = new Date();

            Date goodBeforeDate = new Date(license.getGoodBeforeDate());
            if (goodBeforeDate.before(todayDate))
                return FormValidation.error("License has expired. Please get a new license by logging into https://tester.mobileenerlytics.com");

            Date goodAfterDate = new Date(license.getGoodBeforeDate());
            if(goodAfterDate.after(todayDate))
                return FormValidation.error("Please correct system time to verify license");

        } catch(Exception e) {
            return FormValidation.error("Invalid license string: " + licenseString + " : " + e.getMessage());
        }

        return FormValidation.ok("License verified!");
    }

    public static class EagleLicenseProvider extends DeserializingLicenseProvider {
        @Override
        @SuppressFBWarnings({"DM_DEFAULT_ENCODING"})
        protected byte[] getLicenseData(Object o) {
            return Base64.decodeBase64(o.toString().getBytes());
        }
    }
}

