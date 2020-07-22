package cfh.bf;

import static java.awt.GridBagConstraints.*;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class SettingsDialog {

    private static final int MAX_SIZE = 100000;

    private JDialog dialog;
    private JSpinner sizeSpinner;
    private JCheckBox strictBox;
    
    private JButton okButton;
    private JButton cancelButton;
    
    private boolean okPressed = false;

    public SettingsDialog(JFrame mainFrame, int size, boolean strict, boolean loop) {
        assert 1 <= size && size <= MAX_SIZE : size;
        
        sizeSpinner = new JSpinner(new SpinnerNumberModel(size, 1, MAX_SIZE, 100));
        
        strictBox = new JCheckBox();
        strictBox.setSelected(strict);
        
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.anchor = LINE_START;
        gbcLabel.gridx = 0;
        
        GridBagConstraints gbcField = new GridBagConstraints();
        gbcField.anchor = LINE_START;
        gbcField.gridwidth = REMAINDER;
        gbcField.insets.left = 8;
        gbcField.insets.bottom = 8;
        
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setLayout(new GridBagLayout());
        
        panel.add(new JLabel("Size:"), gbcLabel);
        panel.add(sizeSpinner, gbcField);
        
        panel.add(new JLabel("Strict:"), gbcLabel);
        panel.add(strictBox, gbcField);
        
        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed = true;
                dialog.dispose();
                dialog = null;
            }
        });
        
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed = false;
                dialog.dispose();
                dialog = null;
            }
        });
        
        Box keys = Box.createHorizontalBox();
        keys.add(Box.createHorizontalGlue());
        keys.add(okButton);
        keys.add(Box.createHorizontalStrut(8));
        keys.add(cancelButton);
        keys.add(Box.createHorizontalGlue());
        keys.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        
        dialog = new JDialog(mainFrame, "Brainf*** - Settings", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(keys, BorderLayout.AFTER_LAST_LINE);
        dialog.pack();
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }
    
    public boolean isOkPressed() {
        return okPressed;
    }
    
    public int getSize() {
        return (Integer) sizeSpinner.getValue();
    }
    
    public boolean getStrict() {
        return strictBox.isSelected();
    }

    public boolean getLoop() {
        return getStrict();
    }
}
