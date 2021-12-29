/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.samples.litho.kotlin.lithography.data

object DataCreator {
  fun createPageOfData(baseYear: Int): List<Decade> =
      listOf(
          Decade(
              year = 1800 + baseYear,
              artists =
                  listOf(
                      Artist(
                          name = "Richard James Lane",
                          biography =
                              "Lithographer to Queen Victoria and Prince " +
                                  "Albert, Lane produced over a thousand prints of " +
                                  "his various lithographs of several hundred " +
                                  "portraitures.  One of his better known works is " +
                                  "of a eighteen year old Queen Victoria.",
                          images =
                              listOf(
                                  "https://upload.wikimedia.org/wikipedia/commons" +
                                      "/3/33/George_Francis_Lyon.jpg")),
                      Artist(
                          name = "Louis and Fritz Wolff",
                          biography =
                              "Deaf brothers from Heilbronn, Louis (Ludwig)" +
                                  " and Fritz (Fredrich) Wolff were nineteenth " +
                                  "century lithographers who composed scenes of " +
                                  "buildings, squares and everyday urban life.",
                          images =
                              listOf(
                                  "https://upload.wikimedia.org/wikipedia/commons" +
                                      "/thumb/8/82/B\u00F6ckingen_am_See_1848_" +
                                      "Gebr_Wolff.jpg/512px-B\u00F6ckingen_am_" +
                                      "See_1848_Gebr_Wolff.jpg")))),
          Decade(
              year = 1810 + baseYear,
              artists =
                  listOf(
                      Artist(
                          name = "\u00C9mile Lassalle",
                          biography =
                              "A recipient of the French National Order of " +
                                  "the Legion of Honour and a student of Pierre " +
                                  "Lacour, Lassalle was a painter and lithographer " +
                                  "in the mid-1800s.",
                          images =
                              listOf(
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "thumb/4/4a/Atlas_pittoresque_pl_004.jpg/" +
                                      "512px-Atlas_pittoresque_pl_004.jpg",
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "thumb/7/72/Atlas_pittoresque_pl_119.jpg/" +
                                      "512px-Atlas_pittoresque_pl_119.jpg")),
                      Artist(
                          name = "Jule Arnout",
                          biography =
                              "Jule Arnout, pupil of his father, Jean " +
                                  "Baptiste Arnout, captured " +
                                  "landscapes, " +
                                  "monuments and cities from France, Switzerland, " +
                                  "Italy and England.",
                          images =
                              listOf(
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "0/0e/Sommerset_house_by_ARNOUT%2C_LOUIS" +
                                      "_JULES_-_GMII.jpg",
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "2/2d/Arnout_Boulevard_St_Martin.jpg",
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "a/ac/Jules_Arnout_Saint_Isaac%27" +
                                      "s_Cathedral.jpg",
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "1/1a/Trafalgar_square_by_ARNOUT%2C_LOUIS" +
                                      "_JULES_-_GMII.jpg",
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "8/84/Windsor_castle_by_ARNOUT%2C_LOUIS_" +
                                      "JULES_-_GMII.jpg")),
                      Artist(
                          name = "Anastas Jovanovi\u0107",
                          biography =
                              "Serbian Jovanovi\u0107 is best known as a " +
                                  "historic and pioneering photographer, but his " +
                                  "efforts to capture his hometown of Belgrade " +
                                  "started with paint and lithography.",
                          images =
                              listOf(
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "thumb/2/21/Victory_of_King_Milutin_over_" +
                                      "the_Tatars%2C_Anastas_Jovanovi\u0107_" +
                                      "%281853%29.jpg/512px-Victory_of_King_" +
                                      "Milutin_over_the_Tatars%2C_Anastas_" +
                                      "Jovanovi\u0107_%281853%29.jpg")))),
          Decade(
              year = 1830 + baseYear,
              artists =
                  listOf(
                      Artist(
                          name = "Gaston Marichal",
                          biography =
                              "Little is known about this French " +
                                  "lithographer, except that he worked in Spain " +
                                  "in the latter half of the  1800s.",
                          images =
                              listOf(
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "thumb/9/94/Iglesia_Compa\u00F1ia_Chile" +
                                      ".JPG/512px-Iglesia_Compa\u00F1ia_" +
                                      "Chile.JPG")))),
          Decade(
              year = 1850 + baseYear,
              artists =
                  listOf(
                      Artist(
                          name = "Vincent van Gogh",
                          biography =
                              "The renowned post-impressionist painter " +
                                  "dabbled briefly in lithography, producing ten " +
                                  "works in 1882/3.  Many of his prints, such as" +
                                  " \"Sorrow\" and \"At Eternity's Gate\" he also" +
                                  " captured in other media.",
                          images =
                              listOf(
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "thumb/6/61/Van_Gogh_-_In_the_Orchard_-_" +
                                      "1883.jpg/512px-Van_Gogh_-_In_the_Orchard" +
                                      "_-_1883.jpg",
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "thumb/0/0e/Vincent_Van_Gogh_27.JPG/512px" +
                                      "-Vincent_Van_Gogh_27.JPG")),
                      Artist(
                          name = "Juan Comba Garc\u00EDa",
                          biography =
                              "A Spanish cartoonist, photographer and " +
                                  "painter, Juan Comba Garc\u00EDa was bestowed " +
                                  "the title of \"Graphic Chronicler of the " +
                                  "Restoration\" due to his 683 graphic works " +
                                  "during the time of King Alfonso XII.",
                          images =
                              listOf(
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "c/cf/Duque_de_Sesto_pide_la_mano_de_" +
                                      "Mar%C3%ADa_de_las_Mercedes_de_Orleans.jpg",
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "thumb/2/29/Inauguraci\u00F3n_de_la_" +
                                      "Estaci\u00F3n_definitiva_del_ferrocarril" +
                                      "_de_Madrid_a_Ciudad_Real_y_Badajoz_%28" +
                                      "Comba%29.jpg/512px-Inauguraci\u00F3n_de" +
                                      "_la_Estaci\u00F3n_definitiva_del_" +
                                      "ferrocarril_de_Madrid_a_Ciudad_" +
                                      "Real_y_Badajoz_%28Comba%29.jpg",
                                  "https://upload.wikimedia.org/wikipedia/commons/" +
                                      "thumb/3/3f/Palacio_del_pardo_XIX.jpg/" +
                                      "512px-Palacio_del_pardo_XIX.jpg")))))
}
