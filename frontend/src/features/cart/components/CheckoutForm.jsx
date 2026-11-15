import { Alert, Button, Checkbox, Grid, NumberInput, Radio, SegmentedControl, Select, Stack, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useDebouncedValue } from '@mantine/hooks';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { applyFormErrors } from '../../../shared/api/errors.js';
import { useInstitutions } from '../../../shared/hooks/useInstitutions.js';
import {
  CHECKOUT, DELIVERY_METHOD, INSTITUTION_TYPE, PAYMENT_METHOD, TERMS, labelOf, toOptions,
} from '../../../shared/labels.js';
import { notifyError, notifySuccess } from '../../../shared/notify.js';
import { ErrorState, FormActions, PageLoader, Section } from '../../../shared/ui/index.js';
import { myOrderPath } from '../../orders/index.js';
import { CART_KEY, useCreateOrder, useDeliveryCities, useDeliveryWarehouses, useOrderPreview } from '../api.js';
import { OrderSummary } from './OrderSummary.jsx';
import { TransferRequisites } from './TransferRequisites.jsx';

const REGULAR = 'REGULAR';
const GIFT = 'GIFT';

export function CheckoutForm({ user }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [blocked, setBlocked] = useState(null);
  const [conflict, setConflict] = useState(false);
  const [selectedCityRef, setSelectedCityRef] = useState(null);
  const [selectedWarehouseRef, setSelectedWarehouseRef] = useState(null);
  const [manualPostOffice, setManualPostOffice] = useState(false);

  const cities = useDeliveryCities();
  const warehouses = useDeliveryWarehouses(selectedCityRef);

  const form = useForm({
    initialValues: {
      mode: REGULAR,
      deliveryMethod: 'POST_OFFICE',
      // Filled by the city/branch pickers; the saved address belongs to courier delivery.
      address: '',
      contacts: { recipientName: user.fullName ?? '', phone: user.phone ?? '' },
      paymentMethod: 'ON_DELIVERY',
      institutionId: null,
      bonusPoints: 0,
    },
    validate: {
      address: (v, values) => {
        if (values.mode !== REGULAR || v.trim()) return null;
        return values.deliveryMethod === 'COURIER' ? CHECKOUT.required.address : CHECKOUT.required.postOffice;
      },
      contacts: {
        recipientName: (v, values) => (values.mode === REGULAR && !v.trim() ? CHECKOUT.required.recipientName : null),
        // Same pattern as the backend contract (openapi.json, contacts.phone).
        phone: (v, values) => (values.mode === REGULAR && !/^\+?[0-9]{10,15}$/.test(v.trim()) ? CHECKOUT.required.phone : null),
      },
      institutionId: (v, values) => (values.mode === GIFT && !v ? CHECKOUT.required.institution : null),
    },
  });

  const isGift = form.values.mode === GIFT;
  const paymentMethod = isGift ? 'TRANSFER' : form.values.paymentMethod;
  const bonusPoints = Number(form.values.bonusPoints) || 0;

  const [debouncedPoints] = useDebouncedValue(bonusPoints, 300);
  const preview = useOrderPreview({ bonusPoints: debouncedPoints });
  const institutions = useInstitutions();
  const createOrder = useCreateOrder({ gift: isGift });

  const maxPoints = preview.data?.maxBonusPoints ?? 0;
  const hasUnavailable = preview.data?.items.some((item) => !item.available) ?? false;
  const institution = institutions.data?.find((i) => String(i.id) === form.values.institutionId);

  const handleError = (error) => {
    if (error.is('account-blocked')) {
      setBlocked(error);
      return;
    }
    if (error.status === 409) {
      setConflict(true);
      queryClient.invalidateQueries({ queryKey: CART_KEY });
      queryClient.invalidateQueries({ queryKey: ['orderPreview'] });
      return;
    }
    if (!applyFormErrors(form, error)) notifyError(error);
  };

  const submit = form.onSubmit((values) => {
    setBlocked(null);
    setConflict(false);
    const body = isGift
      ? { institutionId: Number(values.institutionId), paymentMethod: 'TRANSFER', bonusPoints }
      : {
          deliveryMethod: values.deliveryMethod,
          address: values.address.trim(),
          contacts: { recipientName: values.contacts.recipientName.trim(), phone: values.contacts.phone.trim() },
          paymentMethod: values.paymentMethod,
          bonusPoints,
        };
    createOrder.mutate(body, {
      onSuccess: (order) => {
        notifySuccess(CHECKOUT.created);
        navigate(myOrderPath(order.id));
      },
      onError: handleError,
    });
  });

  return (
    <form onSubmit={submit} noValidate>
      <Grid gutter="md">
        <Grid.Col span={{ base: 12, md: 8 }}>
        <Stack gap="md">
          {blocked && (
            <Alert color="red" title={CHECKOUT.blockedTitle}>
              {CHECKOUT.blockedText} {blocked.blockReason && `${CHECKOUT.blockReason} ${blocked.blockReason}`}
            </Alert>
          )}
          {conflict && (
            <Alert color="red" title={CHECKOUT.conflictTitle}>
              {CHECKOUT.conflictText}{' '}
              <Button component={Link} to="/cart" variant="subtle" size="compact-sm">{CHECKOUT.backToCart}</Button>
            </Alert>
          )}
          {hasUnavailable && (
            <Alert color="red" title={CHECKOUT.unavailableTitle}>
              {CHECKOUT.unavailableText}{' '}
              <Button component={Link} to="/cart" variant="subtle" size="compact-sm">{CHECKOUT.backToCart}</Button>
            </Alert>
          )}

          <Section title={CHECKOUT.orderType}>
            <SegmentedControl
              fullWidth
              aria-label={CHECKOUT.orderType}
              data={[{ value: REGULAR, label: CHECKOUT.typeRegular }, { value: GIFT, label: CHECKOUT.typeGift }]}
              {...form.getInputProps('mode')}
            />
          </Section>

          {isGift ? (
            <Section title={CHECKOUT.giftSection}>
              {institutions.isPending ? (
                <PageLoader minHeight={80} />
              ) : institutions.isError ? (
                <ErrorState error={institutions.error} onRetry={institutions.refetch} />
              ) : (
                <>
                  <Select
                    label={CHECKOUT.institution}
                    placeholder={CHECKOUT.institutionPlaceholder}
                    searchable
                    allowDeselect={false}
                    data={institutions.data.map((i) => ({
                      value: String(i.id),
                      label: `${i.name} (${labelOf(INSTITUTION_TYPE, i.type)})`,
                    }))}
                    {...form.getInputProps('institutionId')}
                  />
                  {institution && (
                    <Stack gap={2}>
                      <Text size="sm">{institution.address}</Text>
                      <Text size="sm" c="dimmed">{CHECKOUT.institutionContact}: {institution.contactPerson}</Text>
                    </Stack>
                  )}
                </>
              )}
            </Section>
          ) : (
            <>
              <Section title={CHECKOUT.deliverySection}>
                <Select
                  label={CHECKOUT.deliveryMethod}
                  allowDeselect={false}
                  data={toOptions(DELIVERY_METHOD)}
                  {...form.getInputProps('deliveryMethod')}
                  onChange={(method) => {
                    form.setFieldValue('deliveryMethod', method);
                    // The two methods use different address formats: never carry one over to the other.
                    form.setFieldValue('address', method === 'COURIER' ? (user.deliveryAddress ?? '') : '');
                    form.clearFieldError('address');
                  }}
                />
                {form.values.deliveryMethod === 'COURIER' ? (
                  <TextInput
                    label={CHECKOUT.address}
                    placeholder={CHECKOUT.addressPlaceholder}
                    autoComplete="street-address"
                    {...form.getInputProps('address')}
                  />
                ) : (
                  <>
                    {!manualPostOffice ? (
                      <>
                        <Select
                          label={CHECKOUT.city}
                          placeholder={CHECKOUT.cityPlaceholder}
                          searchable
                          clearable
                          data={(cities.data ?? []).map((c) => ({
                            value: c.ref,
                            label: c.displayName,
                          }))}
                          value={selectedCityRef}
                          onChange={(cityRef) => {
                            setSelectedCityRef(cityRef);
                            setSelectedWarehouseRef(null);
                            form.setFieldValue('address', '');
                          }}
                        />
                        <Select
                          label={CHECKOUT.warehouse}
                          placeholder={selectedCityRef ? CHECKOUT.warehousePlaceholder : CHECKOUT.warehouseSelectCityFirst}
                          searchable
                          clearable
                          disabled={!selectedCityRef || warehouses.isPending}
                          data={(warehouses.data ?? []).map((w) => ({
                            value: w.ref,
                            label: w.label,
                          }))}
                          value={selectedWarehouseRef}
                          onChange={(warehouseRef) => {
                            setSelectedWarehouseRef(warehouseRef);
                            const found = warehouses.data?.find((w) => w.ref === warehouseRef);
                            if (found) {
                              form.setFieldValue('address', found.formattedAddress || found.label);
                            } else {
                              form.setFieldValue('address', '');
                            }
                          }}
                          error={form.errors.address}
                        />
                      </>
                    ) : (
                      <TextInput
                        label={CHECKOUT.address}
                        placeholder={CHECKOUT.manualAddressPlaceholder}
                        {...form.getInputProps('address')}
                      />
                    )}
                    <Checkbox
                      label={CHECKOUT.manualAddressToggle}
                      description={CHECKOUT.manualAddressHint}
                      checked={manualPostOffice}
                      onChange={(e) => {
                        const checked = e.currentTarget.checked;
                        setManualPostOffice(checked);
                        if (!checked) {
                          const found = warehouses.data?.find((w) => w.ref === selectedWarehouseRef);
                          form.setFieldValue('address', found ? (found.formattedAddress || found.label) : '');
                        }
                      }}
                    />
                  </>
                )}
              </Section>
              <Section title={CHECKOUT.contactsSection}>
                <TextInput
                  label={CHECKOUT.recipientName}
                  placeholder={CHECKOUT.recipientNamePlaceholder}
                  autoComplete="name"
                  {...form.getInputProps('contacts.recipientName')}
                />
                <TextInput
                  label={CHECKOUT.phone}
                  placeholder={CHECKOUT.phonePlaceholder}
                  type="tel"
                  autoComplete="tel"
                  {...form.getInputProps('contacts.phone')}
                />
              </Section>
            </>
          )}

          <Section title={CHECKOUT.paymentSection}>
            {isGift ? (
              <>
                <Text fw={600}>{labelOf(PAYMENT_METHOD, 'TRANSFER')}</Text>
                <Text size="sm" c="dimmed">{CHECKOUT.giftPaymentHint}</Text>
              </>
            ) : (
              <Radio.Group label={CHECKOUT.paymentMethod} {...form.getInputProps('paymentMethod')}>
                <Stack gap="xs" mt="xs">
                  {toOptions(PAYMENT_METHOD).map((o) => (
                    <Radio key={o.value} value={o.value} label={o.label} />
                  ))}
                </Stack>
              </Radio.Group>
            )}
          </Section>

          {paymentMethod === 'TRANSFER' && <TransferRequisites />}

          <Section title={CHECKOUT.bonusSection}>
            <NumberInput
              label={CHECKOUT.bonusPoints}
              description={`${CHECKOUT.bonusBalance}: ${user.bonusBalance ?? 0} · ${CHECKOUT.bonusMax}: ${maxPoints}. ${CHECKOUT.bonusLimitHint}`}
              min={0}
              max={maxPoints}
              step={1}
              allowDecimal={false}
              allowNegative={false}
              clampBehavior="strict"
              disabled={maxPoints === 0}
              suffix={` ${TERMS.bonusPoints}`}
              {...form.getInputProps('bonusPoints')}
            />
          </Section>
        </Stack>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 4 }}>
        <Stack gap="md">
          {preview.isError && !preview.data ? (
            <ErrorState error={preview.error} onRetry={preview.refetch} />
          ) : !preview.data ? (
            <PageLoader minHeight={160} />
          ) : (
            <OrderSummary preview={preview.data} />
          )}
          <FormActions
            submitLabel={CHECKOUT.checkoutAction}
            loading={createOrder.isPending}
            disabled={!preview.data || hasUnavailable}
            disabledReason={hasUnavailable ? CHECKOUT.submitUnavailableReason : undefined}
          />
        </Stack>
        </Grid.Col>
      </Grid>
    </form>
  );
}
