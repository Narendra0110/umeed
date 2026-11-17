import { z } from 'zod';

import { ITEM_CONDITIONS, REPORT_REASONS } from './types';

/**
 * Form rules. They repeat the backend's validation (CreateDonationRequest, CreateClaimRequest,
 * CreateReportRequest) so that mistakes are shown before anything is sent. The backend stays the
 * final check.
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
    .min(1, 'Add at least one photo')
    .max(MAX_PHOTOS, `You can add at most ${MAX_PHOTOS} photos`),
  title: z
    .string()
    .trim()
    .min(3, 'Title must be between 3 and 100 characters')
    .max(100, 'Title must be between 3 and 100 characters'),
  description: z
    .string()
    .trim()
    .min(10, 'Description must be between 10 and 1000 characters')
    .max(1000, 'Description must be between 10 and 1000 characters'),
  categoryId: z.string().min(1, 'Choose a category'),
  itemCondition: z.enum(ITEM_CONDITIONS, 'Choose the condition'),
  city: z.string().trim().min(1, 'City is required').max(100, 'City must be at most 100 characters'),
  area: z.string().trim().min(1, 'Area is required').max(100, 'Area must be at most 100 characters'),
  pickupAddress: z
    .string()
    .trim()
    .min(10, 'Pickup address must be between 10 and 500 characters')
    .max(500, 'Pickup address must be between 10 and 500 characters'),
});
export type DonationFormValues = z.input<typeof donationSchema>;
export type DonationInput = z.output<typeof donationSchema>;

export const claimSchema = z.object({
  message: z.string().trim().max(300, 'Message must be at most 300 characters'),
});
export type ClaimFormValues = z.infer<typeof claimSchema>;

export const reportSchema = z
  .object({
    reason: z.enum(REPORT_REASONS, 'Choose a reason'),
    details: z.string().trim().max(500, 'Details must be at most 500 characters'),
  })
  .superRefine((value, ctx) => {
    if (value.reason === 'OTHER' && value.details.length === 0) {
      ctx.addIssue({ code: 'custom', path: ['details'], message: 'Please describe the problem' });
    }
  });
export type ReportFormValues = z.input<typeof reportSchema>;
