package amidst.gui.main.menu;

import java.util.Objects;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import amidst.documentation.Immutable;
import amidst.settings.Setting;

/**
 * This creates only a one way data binding. If the setting is updated, this
 * will not be reflected on the UI. However, this should be good enough for
 * Amidst, since the Settings are only updated via the menu.
 */
@Immutable
public enum Menus {
	;

	public static void radios(JMenu menu, Setting<String> setting,
			String... values) {
		Objects.requireNonNull(values);
		ButtonGroup group = new ButtonGroup();
		for (int i = 0; i < values.length; i++) {
			radio(menu, setting, group, values[i]);
		}
	}

	public static <T> JRadioButtonMenuItem radio(JMenu menu,
			Setting<T> setting, ButtonGroup group, T value) {
		JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
				value.toString());
		return radio(menu, setting, group, menuItem, value);
	}

	public static <T> JRadioButtonMenuItem radio(JMenu menu,
			Setting<T> setting, ButtonGroup group, T value, ImageIcon icon) {
		JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
				value.toString(), icon);
		return radio(menu, setting, group, menuItem, value);
	}

	public static <T> JRadioButtonMenuItem radio(JMenu menu,
			Setting<T> setting, ButtonGroup group, T value, String accelerator) {
		JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
				value.toString());
		menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
		return radio(menu, setting, group, menuItem, value);
	}

	public static <T> JRadioButtonMenuItem radio(JMenu menu,
			Setting<T> setting, ButtonGroup group, T value, ImageIcon icon,
			String accelerator) {
		JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(
				value.toString(), icon);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
		return radio(menu, setting, group, menuItem, value);
	}

	private static <T> JRadioButtonMenuItem radio(JMenu menu,
			Setting<T> setting, ButtonGroup group,
			JRadioButtonMenuItem menuItem, T value) {
		Objects.requireNonNull(value);
		menuItem.addActionListener(e -> setting.set(value));
		menuItem.setSelected(value.equals(setting.get()));
		group.add(menuItem);
		menu.add(menuItem);
		return menuItem;
	}

	public static JCheckBoxMenuItem checkbox(JMenu menu,
			Setting<Boolean> setting, String text) {
		return checkbox(menu, setting, new JCheckBoxMenuItem(text));
	}

	public static JCheckBoxMenuItem checkbox(JMenu menu,
			Setting<Boolean> setting, String text, ImageIcon icon) {
		return checkbox(menu, setting, new JCheckBoxMenuItem(text, icon));
	}

	public static JCheckBoxMenuItem checkbox(JMenu menu,
			Setting<Boolean> setting, String text, String accelerator) {
		JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
		return checkbox(menu, setting, menuItem);
	}

	public static JCheckBoxMenuItem checkbox(JMenu menu,
			Setting<Boolean> setting, String text, ImageIcon icon,
			String accelerator) {
		JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text, icon);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
		return checkbox(menu, setting, menuItem);
	}

	private static JCheckBoxMenuItem checkbox(JMenu menu,
			Setting<Boolean> setting, JCheckBoxMenuItem menuItem) {
		menuItem.setSelected(setting.get());
		menuItem.addActionListener(e -> setting.set(menuItem.isSelected()));
		menu.add(menuItem);
		return menuItem;
	}

	public static JMenuItem item(JMenu menu, Runnable runnable, String text,
			int mnemonic) {
		JMenuItem menuItem = new JMenuItem(text);
		return item(menu, runnable, menuItem, mnemonic);
	}

	public static JMenuItem item(JMenu menu, Runnable runnable, String text,
			int mnemonic, String accelerator) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
		return item(menu, runnable, menuItem, mnemonic);
	}

	private static JMenuItem item(JMenu menu, Runnable runnable,
			JMenuItem menuItem, int mnemonic) {
		menuItem.setMnemonic(mnemonic);
		menuItem.addActionListener(e -> runnable.run());
		menu.add(menuItem);
		return menuItem;
	}
}
