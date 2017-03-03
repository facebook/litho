// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.kittens;

public class DataModel {

  public final String title;
  public final String description;
  public final String[] images;

  public DataModel(String title, String description, String... images) {
    this.title = title;
    this.description = description;
    this.images = images;
  }

  public static DataModel[] SampleData() {
    return new DataModel[] {
      new DataModel(
          "One kitty",
          "This kitty is all alone",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg"
      ), new DataModel(
          "Two kitties",
          "Two kitties is cuter than one kitty",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg"
      ), new DataModel(
          "Three kitties",
          "Three kitties! I can't take the cuteness",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg"
      ), new DataModel(
          "Four kitties",
          "Four kitties are the solution to all the problems in the world",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg"
      ), new DataModel(
          "Five kitties",
          "I don't think it is physically possible to have more kitties",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg",
          "http://thepandorasociety.com/wp-content/uploads/2015/04/PT7-2.jpg"
      )
    };
  }
}
