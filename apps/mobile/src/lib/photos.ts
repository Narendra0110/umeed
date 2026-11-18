import { ImageManipulator, SaveFormat } from 'expo-image-manipulator';
import * as ImagePicker from 'expo-image-picker';

import { t } from './i18n';
import type { Photo } from './schemas';

/** Longest side of an uploaded photo. Phone photos are much larger and would pass the 5 MB limit. */
const MAX_SIDE = 1600;
const JPEG_QUALITY = 0.7;

export type PickResult = { photos: Photo[] } | { error: string };

/** Lets the user choose up to `limit` photos from the gallery, then shrinks them. */
export async function pickFromGallery(limit: number): Promise<PickResult> {
  const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
  if (!permission.granted) {
    return { error: t().donate.galleryDenied };
  }
  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ['images'],
    allowsMultipleSelection: true,
    selectionLimit: limit,
    quality: 1,
  });
  if (result.canceled) {
    return { photos: [] };
  }
  return { photos: await Promise.all(result.assets.slice(0, limit).map(shrink)) };
}

/** Opens the camera for one photo, then shrinks it. */
export async function takePhoto(): Promise<PickResult> {
  const permission = await ImagePicker.requestCameraPermissionsAsync();
  if (!permission.granted) {
    return { error: t().donate.cameraDenied };
  }
  const result = await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 1 });
  if (result.canceled) {
    return { photos: [] };
  }
  return { photos: [await shrink(result.assets[0])] };
}

/** Resizes so the longest side is at most MAX_SIDE and saves as JPEG. */
async function shrink(asset: ImagePicker.ImagePickerAsset): Promise<Photo> {
  const context = ImageManipulator.manipulate(asset.uri);
  if (Math.max(asset.width, asset.height) > MAX_SIDE) {
    context.resize(asset.width >= asset.height ? { width: MAX_SIDE } : { height: MAX_SIDE });
  }
  const image = await context.renderAsync();
  const saved = await image.saveAsync({ compress: JPEG_QUALITY, format: SaveFormat.JPEG });
  return { uri: saved.uri, name: `photo-${Date.now()}-${Math.round(Math.random() * 1e6)}.jpg`, type: 'image/jpeg' };
}
