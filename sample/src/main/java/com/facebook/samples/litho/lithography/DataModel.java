/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentInfo;
import com.facebook.litho.widget.RecyclerBinder;

public class DataModel {

  private static Datum[] SampleData() {
    return new Datum[]{
        new Decade(1800),
        new Artist(
            "Richard James Lane",
            "Lithographer to Queen Victoria and Prince Albert, Lane produced over a " +
                "thousand prints of his various lithographs of several hundred portraitures.  " +
                "One of his better known works is " +
                "of a eighteen year old Queen Victoria.",
            1800,
            "https://upload.wikimedia.org/wikipedia/commons/3/33/George_Francis_Lyon.jpg"
        ),
        new Artist(
            "Louis and Fritz Wolff",
            "Deaf brothers from Heilbronn, Louis (Ludwig) and Fritz (Fredrich) Wolff " +
                "were nineteenth century lithographers who composed scenes of buildings, squares " +
                "and everyday urban life.",
            1802,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/B\u00F6ckingen_am_See_1848_Gebr_Wolff.jpg/512px-B\u00F6ckingen_am_See_1848_Gebr_Wolff.jpg"
        ),
        new Decade(1810),
        new Artist(
            "\u00C9mile Lassalle",
            "A recipient of the French National Order of the Legion of Honour and a " +
                "student of Pierre Lacour, Lassalle was a painter and lithographer in the " +
                "mid-1800s.",
            1813,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Atlas_pittoresque_pl_004.jpg/512px-Atlas_pittoresque_pl_004.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/72/Atlas_pittoresque_pl_119.jpg/512px-Atlas_pittoresque_pl_119.jpg"
        ),
        new Artist(
            "Jule Arnout",
            "Jule Arnout, pupil of his father, Jean Baptiste Arnout, captured " +
                "landscapes, monuments and cities from France, Switzerland, Italy and England.",
            1814,
            "https://upload.wikimedia.org/wikipedia/commons/0/0e/Sommerset_house_by_ARNOUT%2C_LOUIS_JULES_-_GMII.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/2/2d/Arnout_Boulevard_St_Martin.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/a/ac/Jules_Arnout_Saint_Isaac%27s_Cathedral.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/1/1a/Trafalgar_square_by_ARNOUT%2C_LOUIS_JULES_-_GMII.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/8/84/Windsor_castle_by_ARNOUT%2C_LOUIS_JULES_-_GMII.jpg"
        ),
        new Artist(
            "Anastas Jovanovi\u0107",
            "Serbian Jovanovi\u0107 is best known as a historic and pioneering " +
                "photographer, but his efforts to capture his hometown of Belgrade started with " +
                "paint and lithography.",
            1817,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/21/Victory_of_King_Milutin_over_the_Tatars%2C_Anastas_Jovanovi\u0107_%281853%29.jpg/512px-Victory_of_King_Milutin_over_the_Tatars%2C_Anastas_Jovanovi\u0107_%281853%29.jpg"
        ),
        new Decade(1830),
        new Artist(
            "Gaston Marichal",
            "Little is known about this French lithographer, except that he worked in " +
                "Spain in the latter half of the  1800s.",
            1830,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Iglesia_Compa\u00F1ia_Chile.JPG/512px-Iglesia_Compa\u00F1ia_Chile.JPG"
        ),
        new Decade(1850),
        new Artist(
            "Vincent van Gogh",
            "The renowned post-impressionist painter dabbled briefly in lithography, " +
                "producing ten works in 1882/3.  Many of his prints, such as \"Sorrow\" and \"At " +
                "Eternity's Gate\" he also captured in other media.",
            1853,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/6/61/Van_Gogh_-_In_the_Orchard_-_1883.jpg/512px-Van_Gogh_-_In_the_Orchard_-_1883.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0e/Vincent_Van_Gogh_27.JPG/512px-Vincent_Van_Gogh_27.JPG"
        ),
        new Artist(
            "Juan Comba Garc\u00EDa", //Garcia
            "A Spanish cartoonist, photographer and painter, Juan Comba Garc\u00EDa " +
                "was bestowed the title of \"Graphic Chronicler of the Restoration\" due to his " +
                "683 graphic works during the time of King Alfonso XII.",
            1854,
            "https://upload.wikimedia.org/wikipedia/commons/c/cf/Duque_de_Sesto_pide_la_mano_de_Mar%C3%ADa_de_las_Mercedes_de_Orleans.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/29/Inauguraci\u00F3n_de_la_Estaci\u00F3n_definitiva_del_ferrocarril_de_Madrid_a_Ciudad_Real_y_Badajoz_%28Comba%29.jpg/512px-Inauguraci\u00F3n_de_la_Estaci\u00F3n_definitiva_del_ferrocarril_de_Madrid_a_Ciudad_Real_y_Badajoz_%28Comba%29.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3f/Palacio_del_pardo_XIX.jpg/512px-Palacio_del_pardo_XIX.jpg"
        )
    };
  }

  public static void populateBinderWithSampleData(
      RecyclerBinder recyclerBinder,
      ComponentContext c) {
    final Datum[] dataModels = SampleData();
    for (Datum datum : dataModels) {
      ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
      componentInfoBuilder.component(datum.createComponent(c));
      recyclerBinder.insertItemAt(recyclerBinder.getItemCount(), componentInfoBuilder.build());
    }
  }
