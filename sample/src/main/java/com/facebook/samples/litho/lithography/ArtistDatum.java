package com.facebook.samples.litho.lithography;

import com.facebook.litho.Component;

interface ArtistDatum extends Datum {
  String[] getImages();

  String getBiography();

  String getName();
}
