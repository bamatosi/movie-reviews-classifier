
<?php
/**********************************************
* Training set extraction script
* Take 50 movies from each
*	a) the average ratings (http://www.imdb.com/search/title?at=0&num_votes=10000,&release_date=2000,&sort=user_rating,desc&title_type=feature&user_rating=2.0,8.0)
*	b) weak, but not the worst, ratings (http://www.imdb.com/search/title?at=0&num_votes=10000,&release_date=2000,&sort=user_rating,asc&title_type=feature&user_rating=2.0,8.0)
*	c) good, but not the best, ratings (http://www.imdb.com/search/title?at=0&num_votes=10000,&release_date=2000,&sort=user_rating,desc&start=2000&title_type=feature&user_rating=2.0,8.0)
*/

/* Configuration */
$imdbStartingPoint = [
	'http://www.imdb.com/search/title?at=0&num_votes=10000,&release_date=2000,&sort=user_rating,desc&title_type=feature&user_rating=2.0,8.0'
	,'http://www.imdb.com/search/title?at=0&num_votes=10000,&release_date=2000,&sort=user_rating,asc&title_type=feature&user_rating=2.0,8.0'
	,'http://www.imdb.com/search/title?at=0&num_votes=10000,&release_date=2000,&sort=user_rating,desc&start=2000&title_type=feature&user_rating=2.0,8.0'
];

$imdbCommentPageMask = "http://www.imdb.com/title/#id/reviews?start=#offset";
$outputFile = 'imdb-comments-'.date('Ymd-Hi').".csv";

/* Initialize */
$curl = curl_init();
curl_setopt($curl, CURLOPT_USERAGENT, "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1");
curl_setopt($curl, CURLOPT_FOLLOWLOCATION, TRUE);
curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($curl, CURLOPT_TIMEOUT, 60);
$fh = fopen ($outputFile, "w");

/* Loop over IMDB listings too obtain comments */
foreach ($imdbStartingPoint as $imdbUrl) {
	$result = getUrl($curl, $imdbUrl);
	$movies = getMovies($result);
	foreach ($movies as $movieId) {
		$offsets = [0]; // Initilize offsets with 0 offset
		$commentUrl = $imdbCommentPageMask;
		$commentUrl = str_replace("#id", $movieId, $commentUrl);
		
		for($i=0; $i<count($offsets); $i++) {
			$currentOffset = $offsets[$i];
			$commentUrlWithOffset = str_replace("#offset", $currentOffset, $commentUrl);
			$commentsPage = getUrl($curl, $commentUrlWithOffset);
			
			/* Look for more pages and update offset array */
			$paging = getCommentsPages($commentsPage);
			foreach ($paging as $pageOffset) {
				if (!in_array($pageOffset, $offsets)) {
					array_push($offsets, $pageOffset);
					print "Got new offset for $movieId: $pageOffset. Adding to offsets\n";
				}
			}

			/* Fetch comments*/
			$comments = getComments($commentsPage, $commentUrlWithOffset);

			foreach ($comments as $comment) {
				/* Save only the comments having a declared rating */
				saveComment($fh, $movieId, $commentUrlWithOffset, $comment['title'], $comment['comment'], $comment['rating'], $comment['comment-class']);
				print "Saving comment for movie $movieId from $commentUrlWithOffset\n";
			}
		}
	}
}

/* Cleanup */
fclose($fh);
curl_close($curl);


/****************************************************************************************
* DATA EXTRACTION
*****************************************************************************************/
function getUrl($curl, $url) {
	curl_setopt($curl, CURLOPT_URL, $url);
	$sl = rand(4,15);
	print "Fetching $url (in $sl sec)\n";
	sleep($sl); # Let the IMDB breathe
	$result = curl_exec($curl);
	if(!$result){
	    print 'CURL Error "' . curl_error($curl) . '" occured (code: ' . curl_errno($curl)."). Retry in 120 sec...\n";
	    sleep(120);
	    $result = curl_exec($curl);
	    if(!$result){
	    	die('Consecutive CURL Error "' . curl_error($curl) . '" occured (code: ' . curl_errno($curl)."). Going to die...\n");
	    }
	} else {
		return $result;
	}
}

function getMovies($html) {
	$movies = [];
	if (preg_match_all('/<a href="\/title\/([^\/]+)\/">[^<]+<\/a>/', $html, $matches)) {
		$movies = $matches[1];
	}
	return $movies;
}

function getCommentsPages($html) {
	$pages = [];
	if (preg_match_all('/<a title="\d+" href="reviews\?start=(\d+)">\[\d+\]<\/a>/', $html, $matches)) {
		$pages = $matches[1];
	}
	return $pages;
}

function getComments($html, $url){
	$comments = [];
	if (preg_match_all('/<hr[^>]+>[\w\W]*?<div>([\w\W]*?)<\/div>[\w\W]*?<p>([\w\W]*?)<\/p>/', $html, $matches)) {
		$commentsHeaders = $matches[1];
		$commentsBody = $matches[2];
		
		if (count($commentsHeaders) != count($commentsBody)) {
			print "ERROR: I got non equall number of comments and comments headers. Inspect $url\n";
		} else {
			for ($i=0;$i<count($commentsHeaders);$i++) {
				$title = '';
				$rating = -1;
				$commentClass = 0; # 0 - Undefined
				$comment = '';

				$header = $commentsHeaders[$i];
				if (preg_match('/<h2>([^<]+)<\/h2>/', $header, $matches)) {
					$title = $matches[1];
				}
				if (preg_match('/<img.*?alt="(\d+)\/10"/', $header, $matches)) {
					$rating = $matches[1];
					if ($rating <= 3) {
						$commentClass = 1; # 1 - Poor
					} elseif ($rating > 3 && $rating <=7 ) {
						$commentClass = 2; # 2 - Average
					} elseif ($rating > 7) {
						$commentClass = 3; # 3 - Good
					} else {
						$commentClass = 0; # 0 - Undefined
					}
				}
				$comment = $commentsBody[$i];

				$title = str_replace('"', "'", strip_tags(html_entity_decode(trim($title))));
				$title = preg_replace(array("/\s+/","/\n+/"), ' ', $title);
				$comment = str_replace('"', "'", strip_tags(html_entity_decode(trim($comment))));
				$comment = preg_replace(array("/\s+/","/\n+/"), ' ', $comment);
				array_push($comments, array('title'=>$title, 'comment'=>$comment, 'rating'=>$rating, 'comment-class'=>$commentClass));
			}
		}
	}
	return $comments;
}

function saveComment($fh, $movieId, $commentsPage, $title, $comment, $rating, $class) {
	$line = '"'.$movieId.'";"'.$commentsPage.'";'.$rating.';'.$class.';"'.$title.'";"'.$comment.'"'."\n";
	fwrite($fh,$line);
	return;
}