package cfh.bf;

import java.awt.Dimension;
import java.awt.Window;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

public class HelpDialog {

    private final JDialog dialog;

    public HelpDialog(JFrame owner) {
        JTabbedPane tabs = new JTabbedPane();
        
        ResourceBundle.clearCache();
        ResourceBundle bundle = ResourceBundle.getBundle("cfh/bf/resources/help");
        String[] indexes = bundle.getString("help.index").split("\\s*,\\s*");
        for (String index : indexes) {
            String title = bundle.getString("help.title." + index);
            String text = bundle.getString("help.text." + index);
            tabs.addTab(title, createHelpTab(text));
        }
         
        dialog = new JDialog(owner, "HELP", false);
        dialog.add(tabs);
    }
    
    public void setVisible(boolean visible) {
        if (visible) {
            Window owner = dialog.getOwner();
            Dimension size = owner.getSize();
            size.width -= 150;
            size.height -= 100;
            dialog.setSize(size);
            dialog.validate();
            dialog.setLocationRelativeTo(owner);
        }
        dialog.setVisible(visible);
    }
    
    private JComponent createHelpTab(String text) {
        JEditorPane pane = new JEditorPane("text/html", text);
        pane.setEditable(false);
        pane.setCaretPosition(0);
        return new JScrollPane(pane);
    }
}
