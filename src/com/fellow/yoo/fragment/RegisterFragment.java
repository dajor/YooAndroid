package com.fellow.yoo.fragment;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fellow.yoo.R;
import com.fellow.yoo.model.Country;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.StringUtils;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RegisterFragment extends Fragment {

	public enum Type {
		EDIT, COUNTRY, PHONE, NUMBER
	}

	private Map<String, String> data;
	private List<View> inputViews;
	
	/**
	 * A pointer to the current callbacks instance (the Activity).
	 */
	private RegisterCallbacks mCallbacks;
	
	
	public static RegisterFragment newInstance(Map<String, String> data, int step) {
		RegisterFragment f = new RegisterFragment();
		Bundle args = new Bundle();
		args.putInt("step", step);
		args.putSerializable("data", (Serializable)data);
		f.setArguments(args);
		return f;
	}

	@Override
	@SuppressWarnings("unchecked")
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {

		int step = getArguments().getInt("step");
		data = (Map<String, String>)getArguments().get("data");
		inputViews = new ArrayList<View>();
		
		

		RelativeLayout relLayout = (RelativeLayout)inflater.inflate(R.layout.fragment_register, container, false);

		LinearLayout layout = (LinearLayout) relLayout.findViewById(R.id.layout_step);

		Button btnBack = (Button) relLayout.findViewById(R.id.btnBack);
		Button btnNext = (Button) relLayout.findViewById(R.id.btnNext);

		if (step == 1) {
			addView(layout, Type.EDIT, "nickname", R.string.enter_nickname);
			btnBack.setVisibility(View.INVISIBLE);
		}
		if (step == 2) {
			addView(layout, Type.COUNTRY, "countryCode", R.string.select_your_country); 
			addView(layout, Type.PHONE, "phone", R.string.enter_your_phone_number);
		}
		if (step == 3) {
			addView(layout, Type.NUMBER, "activationCode", R.string.enter_activation_code);
		}

		btnNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (validate()) {
					mCallbacks.onNext(data);
				}
			}
		});

		btnBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				mCallbacks.onPrev(data);
			}
		});


		return relLayout;
	}





	private View addView(LinearLayout layout, Type type, final String field, int displayName) {

		String value = data.get(field);
		// input zone
		View inputView = null;
		if (type == Type.COUNTRY) {

			LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			params2.setMargins(5, 0, 5, 0);
			Button button = new Button(layout.getContext());
			button.setBackgroundResource(R.drawable.button_picklist);
			button.setGravity(Gravity.CENTER_VERTICAL);
			button.setLayoutParams(params2);
			// @SuppressWarnings("deprecation")
			@SuppressWarnings("deprecation")
			Drawable img = getResources().getDrawable(R.drawable.arrow_forward_gray);
			int imgSize = ActivityUtils.dpToPixels(getActivity(), 24);
			img.setBounds(0, 0, imgSize, imgSize);
			button.setCompoundDrawables(null, null, img, null);
			

			inputView = button;
			if (type == Type.COUNTRY) {
				buildCountryPicklist(field, value, button);
			}
		} else {
			inputView = new EditText(layout.getContext());
			EditText editText = (EditText) inputView;
			editText.setSingleLine();

			if (type == Type.PHONE) {
				editText.setInputType(InputType.TYPE_CLASS_PHONE);
			} else if (type == Type.NUMBER) {
				editText.setInputType(InputType.TYPE_CLASS_NUMBER);
			} else if (type == Type.EDIT) {
				editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
			}
			if (value != null) {
				editText.setText(value);
			}
			
			editText.requestFocus();
			editText.addTextChangedListener(new TextWatcher() {

				@Override
				public void afterTextChanged(Editable editable) {
					data.put(field, editable.toString());
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
				}
				
			});
			editText.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (v.hasFocus()) {
						int len = ((EditText)v).getEditableText().toString().trim().length();
						if (len > 0) {
							((EditText)v).setSelection((int)len);
						}
					}
				}
			});
			editText.setMaxWidth(100);
		}

		inputView.setTag(field);
		int margin = ActivityUtils.dpToPixels(getActivity(), 10);
		TextView tvLabel = new TextView(layout.getContext(), null, android.R.attr.textAppearanceMedium);
		tvLabel.setText(displayName);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, margin, 0, 0);
		tvLabel.setLayoutParams(params);

		layout.addView(tvLabel);
		layout.addView(inputView);
		
		inputViews.add(inputView);

		return inputView;

	}

	private void buildCountryPicklist(String field, String value, final Button button) {

		final List<Country> countries = getCountries();
		
		if (!StringUtils.isEmpty(value)) {
			for (Country country : countries) {
				if (country.getCode().equals(value)) {
					button.setText(country.getName());
				}
			}
		}

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						displayListCountry(button, countries);
					}
				});
			}
		});
	}


	private void displayListCountry(final Button btnCountry, final List<Country> countryList) {

		final List<String> nameList = new ArrayList<String>();

		for (Country country : countryList) {
			nameList.add(country.getName());
		}

		final Dialog countryDia = new Dialog(getActivity());
		countryDia.setTitle(getString(R.string.list_country));

		ListView countryListview = new ListView(getActivity());
		countryListview.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, nameList));

		TextView errorConnectionTv = new TextView(getActivity());
		errorConnectionTv.setText(getString(R.string.error_connection));
		errorConnectionTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		int padding = ActivityUtils.dpToPixels(getActivity(), 10);
		errorConnectionTv.setPadding(padding, padding, padding, padding);

		countryListview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		if (nameList.size() > 0) {
			countryDia.setContentView(countryListview);
		} else {
			countryDia.setContentView(errorConnectionTv);
		}

		countryDia.show();

		countryListview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
				btnCountry.setText(nameList.get(position));
				data.put((String)btnCountry.getTag(), countryList.get(position).getCode());
				countryDia.dismiss();
			}
		});
	}

	/**
	 * Callbacks interface that all activities using this fragment must
	 * implement.
	 */
	public static interface RegisterCallbacks {
		/**
		 * Called when next or prev are clicked.
		 */
		void onNext(Map<String, String> values);
		void onPrev(Map<String, String> values);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mCallbacks = (RegisterCallbacks) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(
					"Activity must implement RegisterCallbacks.");
		}
	}
	
	public void wrongCode() {
		for (View view : inputViews) {
			if (view instanceof EditText) {
				EditText editText = (EditText) view;
				if (editText.getTag().equals("activationCode")) {
					editText.setError("DIABLERIE");
					//editText.setError(getActivity().getString(R.string.invalid_code));
				}
			}
		}
	}
	
	private boolean validate() {
		boolean ok = true;
		for (View view : inputViews) {
			if (view instanceof EditText) {
				EditText editText = (EditText) view;
				if (editText.length() == 0) {
					editText.setError(getActivity().getString(R.string.error_field_required));
					ok = false;
				}
				
				if (editText.getTag().equals("phone")
						&& !StringUtils.isEmpty(data.get("phone")) && !StringUtils.isEmpty(data.get("phone"))) {
					PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
					try {
						PhoneNumber numberProto = phoneUtil.parse(data.get("phone"), data.get("countryCode"));
						if (!phoneUtil.isValidNumber(numberProto)) {
							editText.setError(getActivity().getString(R.string.WRONG_NUMBER));
							ok = false;
						} else {
							String formattedPhone = phoneUtil.format(numberProto, PhoneNumberFormat.E164);
							if (formattedPhone.startsWith("+")) {
								formattedPhone = formattedPhone.substring(1);
							}
							data.put("formattedPhone", formattedPhone);
							data.put("callingCode", String.valueOf(numberProto.getCountryCode()));
						}
						
						/*for (String st : phoneUtil.getSupportedRegions()) {
							System.out.println(" >> SupportedRegions >> " + st); 
						}*/
					} catch (NumberParseException e) {
						editText.setError(getActivity().getString(R.string.WRONG_NUMBER));
						Log.i("RegisterFragment", "Wrong phone number", e);
						ok = false;
					}
				}
				
				if (editText.getTag().equals("activationCode")) {
					if (editText.length() != 4) {
						editText.setError(getActivity().getString(R.string.invalid_code));
						ok = false;
					}
				}
			}
		}
		
		return ok;
	}

	private List<Country> getCountries() {
		List<Country> countries = new ArrayList<Country>();
		String[] locales = Locale.getISOCountries();
		for(String code : locales){
			Locale locale = new Locale("", code);
			String country = locale.getDisplayCountry().trim();
			if(country.length() > 0){
				countries.add(new Country(locale.getCountry(), country));
			}
		}
		final Collator collator = Collator.getInstance(Locale.FRENCH);
		collator.setStrength(Collator.PRIMARY);
        Collections.sort(countries, new Comparator<Country>() {
        	@Override
        	public int compare(Country lhs, Country rhs) {
        		return collator.compare(lhs.getName(), rhs.getName());
        	}
		});
        return countries;
	}

}
