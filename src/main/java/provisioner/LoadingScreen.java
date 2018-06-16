package provisioner;

import java.awt.Toolkit;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author andrew
 */
public class LoadingScreen extends javax.swing.JFrame {

    private JPanel panel = new JPanel();
    private BoxLayout box = new BoxLayout(panel, BoxLayout.Y_AXIS);

    /**
     * Create the loading screen shown when program launches
     * @throws Exception 
     */
    public LoadingScreen() throws Exception {

        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Loading...");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/applogo.png")));

        jLabel1.setText("Syncing data and loading program...");
        jLabel1.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        panel.add(jLabel1);
        panel.setLayout(box);
        this.add(panel);
        pack();
        setLocationRelativeTo(null);
        this.setVisible(true);

        MainMenu d = new MainMenu(this);

    }

    public void setText(String text) {
        jLabel1.setText(text);
    }

    public JLabel createLabel(String text) {
        JLabel label = new JLabel();
        label.setText(text);
        label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        panel.add(label);
        panel.repaint();
        pack();
        return label;
    }

    public void updateText(JLabel label, String text, String percent) {
        label.setText(text + percent);
        panel.repaint();
        pack();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws Exception {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(LoadingScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LoadingScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LoadingScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LoadingScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        new LoadingScreen();
        /* Create and display the form */

    }

    // Variables declaration - do not modify                     
    private javax.swing.JLabel jLabel1;
    // End of variables declaration    

}