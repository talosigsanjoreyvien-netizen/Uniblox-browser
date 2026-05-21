package fun.cybercode.uniblox.browser;

import android.content.Context;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class CertificateValidator {
    private static final String CERTIFICATE_FILE = "uc.md";
    private static final String REQUIRED_TOKEN = "[!uniblox-app-verification-token=";
    
    public static boolean validateCertificate(Context context) {
        try {
            // Check if uc.md exists in assets
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(CERTIFICATE_FILE))
            );
            
            StringBuilder content = new StringBuilder();
            String line;
            boolean tokenFound = false;
            
            while ((line = reader.readLine()) != null) {
                content.append(line);
                if (line.contains(REQUIRED_TOKEN)) {
                    tokenFound = true;
                }
            }
            reader.close();
            
            if (!tokenFound) {
                showError(context, "Invalid Certificate", 
                    "Uniblox Corporation certificate not found. \n" +
                    "This application cannot run without proper authorization.");
                return false;
            }
            
            return true;
            
        } catch (IOException e) {
            showError(context, "Certificate Missing", 
                "Uniblox Corporation License Certificate (uc.md) is missing.\n" +
                "Only authorized team members may use this application.");
            return false;
        }
    }
    
    private static void showError(Context context, String title, String message) {
        Toast.makeText(context, title + ": " + message, Toast.LENGTH_LONG).show();
    }
}
