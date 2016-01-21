# Movie Reviews Classifier
This is a showcase OS project utilising Apache Spark to classify movie reviews. The ultimate goal is figure out (in the real time) what is the reception of the particular movie among people using Twitter.

## Training set
Training set can be aquired from freely available web pages and contains a set of movie reviews along with review rating (stars).
Sample script and data set are available under /Data/.

I have used the review rating to split the whole set of reviews into 3 classes
- BAD: stars from 1 to 3, mapped to class value 1.0
- MODERATE: stars from 4 to 7, mapped to class value 2.0
- GOOD: stars from 8 to 10, mapped to class value 3.0

The minimum set of information to train model is: id (integer) unique for every training example, review (string), class (double)

## Building model
Model is build with Spark's mllib. Inspect a code under /Movies/ to gain an understaning on how it works.
I assumed that by using a training set large enought (I have tested it on ~55k reviews) the model can gain a pretty good distinction between GOOD and BAD sentiment which is my ultimate goal.

*Sample output from the model* (for the ~55k training set obtained from IMDB):
- Result for 'the worst movie ever': 1.0 // BAD review
- Result for 'terrible boring but great on the other hand': 2.0 // MODERATE review
- Result for 'pure awesome': 3.0 // GOOD review

## Roadmap
Yes I have a roadmap for this project, why the heck not :).

- ~~Aquire training data (PHP script crawling for IMDB moview reviews)~~
- ~~Build, train and test classifier~~
- Add tokens stemming (like, likes, liked -> like)
- Add support for negation by creating a negated tokens (isn't awesome -> not_awesome)
- Include IDF factor, experiment with different implementations of TF factor (binary, augumented)
- Add streaming from Twitter; the point here is to stream tweets (using movie-specific hashtag like #SPECTRE) and run them through classifier in the real time to gain a general understanding of what is the movie reception among people using Twitter. Training set aquired from the websites may not be a perfect fit for training model for classifying tweets. Nevertheless I think it is worth a shot, especially after adding a support for stemming and negations
- Add a simple web frontend to showcase the sentiment for the movies launched next weekend (http://www.imdb.com/calendar/?region=pl)
- Add a simple admin for managing movie premieres and their hashtags