import { View, Text, StyleSheet } from 'react-native';
import { Colors } from '../constants/colors';

type LogoProps = {
  size?: 'small' | 'large';
};

export function Logo({ size = 'large' }: LogoProps) {
  const isLarge = size === 'large';

  return (
    <View style={[styles.container, isLarge && styles.containerLarge]}>
      <View style={[styles.iconWrap, isLarge && styles.iconWrapLarge]}>
        <View style={[styles.barLeft, isLarge && styles.barLeftLarge]} />
        <View style={[styles.grip, isLarge && styles.gripLarge]} />
        <View style={[styles.barRight, isLarge && styles.barRightLarge]} />
      </View>
      {isLarge && (
        <View style={styles.textWrap}>
          <Text style={styles.logoText}>GYM</Text>
          <Text style={styles.logoTextAccent}>BUDDY</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    gap: 8,
  },
  containerLarge: {
    gap: 14,
  },
  iconWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  iconWrapLarge: {
    gap: 4,
  },
  barLeft: {
    width: 10,
    height: 22,
    backgroundColor: Colors.primary,
    borderRadius: 3,
  },
  barLeftLarge: {
    width: 16,
    height: 36,
    borderRadius: 4,
  },
  grip: {
    width: 24,
    height: 8,
    backgroundColor: Colors.text,
    borderRadius: 4,
  },
  gripLarge: {
    width: 40,
    height: 12,
    borderRadius: 6,
  },
  barRight: {
    width: 10,
    height: 22,
    backgroundColor: Colors.primary,
    borderRadius: 3,
  },
  barRightLarge: {
    width: 16,
    height: 36,
    borderRadius: 4,
  },
  textWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  logoText: {
    fontSize: 28,
    fontWeight: '900',
    color: Colors.text,
    letterSpacing: 4,
  },
  logoTextAccent: {
    fontSize: 28,
    fontWeight: '900',
    color: Colors.primary,
    letterSpacing: 4,
  },
});
