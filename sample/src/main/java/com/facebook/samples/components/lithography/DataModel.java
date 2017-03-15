// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.lithography;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentInfo;
import com.facebook.components.widget.RecyclerBinder;

public class DataModel {

  public final String title;
  public final String description;
  public final String[] images;

  public DataModel(String title, String description, String... images) {
    this.title = title;
    this.description = description;
    this.images = images;
  }

  private static DataModel[] SampleData() {
    return new DataModel[]{
        new DataModel(
            "Louis and Fritz Wolff",
            "Deaf brothers from Heilbronn, Louis (Ludwig) and Fritz (Fredrich) Wolff " +
                "were nineteenth century lithographers who composed scenes of buildings, squares " +
                "and everyday urban life.",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/B\u00F6ckingen_am_See_1848_Gebr_Wolff.jpg/512px-B\u00F6ckingen_am_See_1848_Gebr_Wolff.jpg"
        ),
        new DataModel(
            "Jule Arnout",
            "Jule Arnout, pupil of his father, Jean Baptiste Arnout, captured " +
                "landscapes, monuments and cities from France, Switzerland, Italy and England.",
            "https://upload.wikimedia.org/wikipedia/commons/0/0e/Sommerset_house_by_ARNOUT%2C_LOUIS_JULES_-_GMII.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/2/2d/Arnout_Boulevard_St_Martin.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/a/ac/Jules_Arnout_Saint_Isaac%27s_Cathedral.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/1/1a/Trafalgar_square_by_ARNOUT%2C_LOUIS_JULES_-_GMII.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/8/84/Windsor_castle_by_ARNOUT%2C_LOUIS_JULES_-_GMII.jpg"
        ),
        new DataModel(
            "Juan Comba Garc\u00EDa", //Garcia
            "A Spanish cartoonist, photographer and painter, Juan Comba Garc\u00EDa " +
                "was bestowed the title of \"Graphic Chronicler of the Restoration\" due to his " +
                "683 graphic works during the time of King Alfonso XII.",
            "https://upload.wikimedia.org/wikipedia/commons/c/cf/Duque_de_Sesto_pide_la_mano_de_Mar%C3%ADa_de_las_Mercedes_de_Orleans.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/29/Inauguraci\u00F3n_de_la_Estaci\u00F3n_definitiva_del_ferrocarril_de_Madrid_a_Ciudad_Real_y_Badajoz_%28Comba%29.jpg/512px-Inauguraci\u00F3n_de_la_Estaci\u00F3n_definitiva_del_ferrocarril_de_Madrid_a_Ciudad_Real_y_Badajoz_%28Comba%29.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3f/Palacio_del_pardo_XIX.jpg/512px-Palacio_del_pardo_XIX.jpg"
        ),
        new DataModel(
            "Anastas Jovanovi\u0107",
            "Serbian Jovanovi\u0107 is best known as a historic and pioneering " +
                "photographer, but his efforts to capture his hometown of Belgrade started with " +
                "paint and lithography.",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/21/Victory_of_King_Milutin_over_the_Tatars%2C_Anastas_Jovanovi\u0107_%281853%29.jpg/512px-Victory_of_King_Milutin_over_the_Tatars%2C_Anastas_Jovanovi\u0107_%281853%29.jpg"
        ),
        new DataModel(
            "Richard James Lane",
            "Lithographer to Queen Victoria and Prince Albert, Lane produced over a " +
                "thousand prints of his various lithographs of several hundred portraitures.  " +
                "One of his better known works is " +
                "of a eighteen year old Queen Victoria.",
            "https://upload.wikimedia.org/wikipedia/commons/3/33/George_Francis_Lyon.jpg"
        ),
        new DataModel(
            "\u00C9mile Lassalle",
            "A recipient of the French National Order of the Legion of Honour and a " +
                "student of Pierre Lacour, Lassalle was a painter and lithographer in the " +
                "mid-1800s.",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Atlas_pittoresque_pl_004.jpg/512px-Atlas_pittoresque_pl_004.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/72/Atlas_pittoresque_pl_119.jpg/512px-Atlas_pittoresque_pl_119.jpg"
        ),
        new DataModel(
            "Gaston Marichal",
            "Little is known about this French lithographer, except that he worked in " +
                "Spain in the latter half of the  1800s.",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Iglesia_Compa\u00F1ia_Chile.JPG/512px-Iglesia_Compa\u00F1ia_Chile.JPG"
        ),
        new DataModel(
            "Vincent van Gogh",
            "The renowned post-impressionist painter dabbled briefly in lithography, " +
                "producing ten works in 1882/3.  Many of his prints, such as \"Sorrow\" and \"At " +
                "Eternity's Gate\" he also captured in other media.",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/6/61/Van_Gogh_-_In_the_Orchard_-_1883.jpg/512px-Van_Gogh_-_In_the_Orchard_-_1883.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0e/Vincent_Van_Gogh_27.JPG/512px-Vincent_Van_Gogh_27.JPG"
        )
    };
  }

  public static void populateBinderWithSampleData(
      RecyclerBinder recyclerBinder,
      ComponentContext c) {
    final DataModel[] dataModels = SampleData();
    for (int i = 0; i < dataModels.length; i++) {
      ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
      componentInfoBuilder.component(
          FeedItemComponent.create(c)
              .item(dataModels[i])
              .index(i)
              .build());
      recyclerBinder.insertItemAt(recyclerBinder.getItemCount(), componentInfoBuilder.build());
    }
  }
}
