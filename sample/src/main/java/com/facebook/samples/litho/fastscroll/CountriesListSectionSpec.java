/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.samples.litho.fastscroll;

import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@GroupSectionSpec
class CountriesListSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c) {
    final Children.Builder children = new Children.Builder();

    for (String country : COUNTRIES_LIST) {
      children.child(
          SingleComponentSection.create(c)
              .component(
                  Text.create(c)
                      .text(country)
                      .textSizeDip(22)
                      .paddingDip(YogaEdge.HORIZONTAL, 24)
                      .paddingDip(YogaEdge.VERTICAL, 12)
                      .build())
              .build());
    }

    return children.build();
  }

  static final String[] COUNTRIES_LIST =
      new String[] {
        "Afghanistan",
        "Albania",
        "Algeria",
        "America",
        "Andorra",
        "Angola",
        "Antigua",
        "Argentina",
        "Armenia",
        "Australia",
        "Austria",
        "Azerbaijan",
        "Bahamas",
        "Bahrain",
        "Bangladesh",
        "Barbados",
        "Belarus",
        "Belgium",
        "Belize",
        "Benin",
        "Bhutan",
        "Bissau",
        "Bolivia",
        "Bosnia",
        "Botswana",
        "Brazil",
        "British",
        "Brunei",
        "Bulgaria",
        "Burkina",
        "Burma",
        "Burundi",
        "Cambodia",
        "Cameroon",
        "Canada",
        "Cape Verde",
        "Central African Republic",
        "Chad",
        "Chile",
        "China",
        "Colombia",
        "Comoros",
        "Congo",
        "Costa Rica",
        "Croatia",
        "Cuba",
        "Cyprus",
        "Czech",
        "Denmark",
        "Djibouti",
        "Dominica",
        "East Timor",
        "Ecuador",
        "Egypt",
        "El Salvador",
        "Emirate",
        "England",
        "Eritrea",
        "Estonia",
        "Ethiopia",
        "Fiji",
        "Finland",
        "France",
        "Gabon",
        "Gambia",
        "Georgia",
        "Germany",
        "Ghana",
        "Great Britain",
        "Greece",
        "Grenada",
        "Grenadines",
        "Guatemala",
        "Guinea",
        "Guyana",
        "Haiti",
        "Herzegovina",
        "Holland",
        "Honduras",
        "Hungary",
        "Iceland",
        "India",
        "Indonesia",
        "Iran",
        "Iraq",
        "Ireland",
        "Israel",
        "Italy",
        "Ivory Coast",
        "Jamaica",
        "Japan",
        "Jordan",
        "Kazakhstan",
        "Kenya",
        "Kiribati",
        "Korea",
        "Kosovo",
        "Kuwait",
        "Kyrgyzstan",
        "Laos",
        "Latvia",
        "Lebanon",
        "Lesotho",
        "Liberia",
        "Libya",
        "Liechtenstein",
        "Lithuania",
        "Luxembourg",
        "Macedonia",
        "Madagascar",
        "Malawi",
        "Malaysia",
        "Maldives",
        "Mali",
        "Malta",
        "Marshall",
        "Mauritania",
        "Mauritius",
        "Mexico",
        "Micronesia",
        "Moldova",
        "Monaco",
        "Mongolia",
        "Montenegro",
        "Morocco",
        "Mozambique",
        "Myanmar",
        "Namibia",
        "Nauru",
        "Nepal",
        "Netherlands",
        "New Zealand",
        "Nicaragua",
        "Niger",
        "Nigeria",
        "Norway",
        "Oman",
        "Pakistan",
        "Palau",
        "Panama",
        "Papua",
        "Paraguay",
        "Peru",
        "Philippines",
        "Poland",
        "Portugal",
        "Qatar",
        "Romania",
        "Russia",
        "Rwanda",
        "Samoa",
        "San Marino",
        "Sao Tome",
        "Saudi Arabia",
        "Scotland",
        "Scottish",
        "Senegal",
        "Serbia",
        "Seychelles",
        "Sierra Leone",
        "Singapore",
        "Slovakia",
        "Slovenia",
        "Solomon",
        "Somalia",
        "South Africa",
        "South Sudan",
        "Spain",
        "Sri Lanka",
        "St Kitts",
        "St Lucia",
        "Sudan",
        "Suriname",
        "Swaziland",
        "Sweden",
        "Switzerland",
        "Syria",
        "Taiwan",
        "Tajikistan",
        "Tanzania",
        "Thailand",
        "Tobago",
        "Togo",
        "Tonga",
        "Trinidad",
        "Tunisia",
        "Turkey",
        "Turkmenistan",
        "Tuvalu",
        "Uganda",
        "Ukraine",
        "United Kingdom",
        "United States",
        "Uruguay",
        "USA",
        "Uzbekistan",
        "Vanuatu",
        "Vatican",
        "Venezuela",
        "Vietnam",
        "Wales",
        "Welsh",
        "Yemen",
        "Zambia",
        "Zimbabwe"
      };
}
