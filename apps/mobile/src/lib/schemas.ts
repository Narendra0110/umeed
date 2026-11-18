import { z } from 'zod';

import { msg } from './i18n';
import { ITEM_CONDITIONS, REPORT_REASONS } from './types';

/**
 * Form rules. They repeat the backend's validation (CreateDonationRequest, CreateClaimRequest,
 * CreateReportRequest) so that mistakes are shown before anything is sent. The backend stays the
 * final check. Messages are looked up when a rule runs, so they follow the chosen language.
 */

export const MAX_PHOTOS = 5;

export const photoSchema = z.object({
  uri: z.string().min(1),
  name: z.string().min(1),
  type: z.string().min(1),
});
export type Photo = z.infer<typeof photoSchema>;

export const donationSchema = z.object({
  photos: z
    .array(photoSchema)
    .min(1, msg((m) => m.form.addPhoto))
    .max(MAX_PHOTOS, msg((m) => m.form.maxPhotos)),
  title: z
    .string()
    .trim()
    .min(3, msg((m) => m.form.titleLength))
    .max(100, msg((m) => m.form.titleLength)),
  description: z
    .string()
    .trim()
    .min(10, msg((m) => m.form.descriptionLength))
    .max(1000, msg((m) => m.form.descriptionLength)),
  categoryId: z.string().min(1, msg((m) => m.form.chooseCategory)),
  itemCondition: z.enum(ITEM_CONDITIONS, msg((m) => m.form.chooseCondition)),
  city: z
    .string()
    .trim()
    .min(1, msg((m) => m.form.cityRequired))
    .max(100, msg((m) => m.form.cityTooLong)),
  area: z
    .string()
    .trim()
    .min(1, msg((m) => m.form.areaRequired))
    .max(100, msg((m) => m.form.areaTooLong)),
  pickupAddress: z
    .string()
    .trim()
    .min(10, msg((m) => m.form.addressLength))
    .max(500, msg((m) => m.form.addressLength)),
});
export type DonationFormValues = z.input<typeof donationSchema>;
export type DonationInput = z.output<typeof donationSchema>;

/** Asking for something (CreateNeedRequest): like an item, without photos, condition or address. */
export const needSchema = z.object({
  title: donationSchema.shape.title,
  description: donationSchema.shape.description,
  categoryId: donationSchema.shape.categoryId,
  city: donationSchema.shape.city,
  area: donationSchema.shape.area,
});
export type NeedFormValues = z.input<typeof needSchema>;
export type NeedInput = z.output<typeof needSchema>;

/** Offering an item for a need (CreateOfferRequest). */
export const offerSchema = z.object({
  donationId: z.string().min(1, msg((m) => m.form.chooseItem)),
  message: z
    .string()
    .trim()
    .max(300, msg((m) => m.form.messageTooLong)),
});
export type OfferFormValues = z.infer<typeof offerSchema>;

export const claimSchema = z.object({
  message: z
    .string()
    .trim()
    .max(300, msg((m) => m.form.messageTooLong)),
});
export type ClaimFormValues = z.infer<typeof claimSchema>;

export const reportSchema = z
  .object({
    reason: z.enum(REPORT_REASONS, msg((m) => m.form.chooseReason)),
    details: z
      .string()
      .trim()
      .max(500, msg((m) => m.form.detailsTooLong)),
  })
  .superRefine((value, ctx) => {
    if (value.reason === 'OTHER' && value.details.length === 0) {
      ctx.addIssue({ code: 'custom', path: ['details'], message: msg((m) => m.form.describeProblem).error() });
    }
  });
export type ReportFormValues = z.input<typeof reportSchema>;

/** Login: the 10-digit Indian mobile number without +91. */
export const phoneSchema = z.object({
  phone: z
    .string()
    .trim()
    .regex(/^[6-9][0-9]{9}$/, msg((m) => m.form.phoneDigits)),
});

/** The 6-digit SMS code (login and account deletion). */
export const codeSchema = z.object({
  code: z
    .string()
    .trim()
    .regex(/^[0-9]{6}$/, msg((m) => m.form.codeDigits)),
});

export const displayNameSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(2, msg((m) => m.form.nameTooShort))
    .max(80, msg((m) => m.form.nameTooLong)),
});
